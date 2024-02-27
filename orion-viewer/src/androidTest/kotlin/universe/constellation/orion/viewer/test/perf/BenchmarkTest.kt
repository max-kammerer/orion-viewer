package universe.constellation.orion.viewer.test.perf

import android.graphics.Color
import com.artifex.mupdf.fitz.Context
import com.artifex.mupdf.fitz.StructuredText.TextBlock
import org.junit.After
import org.junit.Before
import org.junit.Test
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.pdf.PdfDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

class BenchmarkTest : BaseTest() {

    data class Info(val book: BookFile, val openTime: Long, val pageInfoAvg: Long, val gotoPageAvg: Long, val displayListAvg: Long, val pureRendering: Long, val closeTime: Long)

    private val bitmap = Bitmap.createBitmap(800, 1024, android.graphics.Bitmap.Config.ARGB_8888)

    @Before
    fun setUp() {
        DjvuDocument.Companion
        Context.init()
    }

    @After
    fun tearDown() {
        bitmap.recycle()
    }

    @Test
    fun benchmark() {
        val infos = BookFile.testEntriesWithCustoms().map {
            val time = time()
            val book = it.openBookNoCacheWrapper()
            val openTime = timeDelta(time)

            val gotoAvg = (1..20).map {
                val goto = time()
                book.goToPageInt(it)
                timeDelta(goto)
            }.average().toLong()

            val pageInfo = (21..40).map {
                val goto = time()
                book.getPageInfo(it)
                timeDelta(goto)
            }.average().toLong()

            val pureRendering = mutableListOf<Long>()

            val displayList = (1..20).map {
                bitmap.eraseColor(Color.TRANSPARENT)
                book.goToPageInt(it)
                val goto = time()
                if (book is PdfDocument) {
                    val displayList = book.createDisplayListForCurrentPage()
                    val res = timeDelta(goto)
                    pureRendering.add(
                        timeDelta {
                            book.renderPage(it, bitmap, 1.0, 0, 0, bitmap.width, bitmap.height, 0, 0)
                        }
                    )
                    displayList?.destroy()
                    res
                } else {
                    pureRendering.add(
                        timeDelta {
                            book.renderPage(it, bitmap, 1.0, 0, 0, bitmap.width, bitmap.height, 0, 0)
                        }
                    )
                    0
                }
            }.average().toLong()

            val close = time()
            book.destroy()
            val closeTime = timeDelta(close)
            Info(it, openTime, pageInfo, gotoAvg, displayList, pureRendering.average().toLong(), closeTime)
        }

        println(infos.joinToString("\n"))
    }

    private fun time() = System.currentTimeMillis()

    private fun timeDelta(start: Long) = System.currentTimeMillis() - start

    private inline fun timeDelta(block: () -> Unit): Long {
        val start = time()
        block()
        return timeDelta(start)
    }

}