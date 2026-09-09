package universe.constellation.orion.viewer.test.perf

import android.util.Log
import com.artifex.mupdf.fitz.Document
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import java.io.File

/* Timing of the two text sources: structured text straight from the page (a content stream
 * run) versus from the display list built for rendering (a list replay). Also the cost of the
 * list itself. Results go to logcat, tag TextTiming. */
class TextSourcesTimingTest : BaseTest() {

    @Test
    fun timeTextSources() {
        val books = listOf(BookDescription.SICP.asPath())
        for (path in books) {
            val document = Document.openDocument(path)
            try {
                val count = document.countPages()
                val pages = PAGES.filter { it < count }
                var pageText = 0L; var listBuild = 0L; var listText = 0L; var chars = 0
                for (pageNum in pages) {
                    val page = document.loadPage(pageNum)
                    try {
                        /* warm up fonts and the store once, the same for both sources */
                        page.toStructuredText().destroy()
                        repeat(RUNS) {
                            pageText += timed { page.toStructuredText().destroy() }
                            var list = timedValue({ page.toDisplayList(true) }) { listBuild += it }
                            listText += timed { list.toStructuredText().also { t -> chars += t.asText().length }.destroy() }
                            list.destroy()
                        }
                    } finally {
                        page.destroy()
                    }
                }
                val n = (pages.size * RUNS).toDouble()
                Log.i(TAG, "%s: pages=%d runs=%d chars/run=%d | page->stext %.2f ms | list build %.2f ms | list->stext %.2f ms".format(
                    File(path).name, pages.size, RUNS, chars / (pages.size * RUNS).coerceAtLeast(1),
                    pageText / n / 1e6, listBuild / n / 1e6, listText / n / 1e6))
                for (pageNum in pages) {
                    val page = document.loadPage(pageNum)
                    try {
                        val a = timed { page.toStructuredText().destroy() }
                        val list = page.toDisplayList(true)
                        val b = timed { list.toStructuredText().destroy() }
                        val stext = list.toStructuredText()
                        var charCount = 0
                        val c = timed {
                            for (block in stext.blocks) for (line in block?.lines ?: continue) for (ch in line?.chars ?: continue) charCount++
                        }
                        stext.destroy()
                        list.destroy()
                        Log.i(TAG, "  %s page %d: page->stext %.2f ms, list->stext %.2f ms, blocks/lines/chars walk %.2f ms (%d chars)".format(File(path).name, pageNum, a / 1e6, b / 1e6, c / 1e6, charCount))
                    } finally {
                        page.destroy()
                    }
                }
            } finally {
                document.destroy()
            }
        }
    }

    private inline fun timed(body: () -> Unit): Long {
        val start = System.nanoTime()
        body()
        return System.nanoTime() - start
    }

    private inline fun <T> timedValue(body: () -> T, record: (Long) -> Unit): T {
        val start = System.nanoTime()
        val result = body()
        record(System.nanoTime() - start)
        return result
    }

    companion object {
        private const val TAG = "TextTiming"
        private const val RUNS = 5
        private val PAGES = listOf(1, 2, 5, 11, 12, 30, 100, 250, 400, 600)
    }
}
