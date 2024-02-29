package universe.constellation.orion.viewer.test.perf

import android.graphics.Color
import com.artifex.mupdf.fitz.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

class BenchmarkTest : BaseTest() {

    class DataList(val list: List<Long>) {
        override fun toString(): String {
            return "" + list.min() + "-" + list.average().toLong() + '-' + list.max() + "("+ list.indexOf(list.max()) + ")"
        }
    }

    class Info(
        val openTime: Long,
        val pageInfoAvg: DataList,
        val readPageData: DataList,
        val pureRendering: DataList,
        val closeTime: Long,
        val book: BookFile
    ) {
        override fun toString(): String {
            return "pageInfo=$pageInfoAvg, readPageData=$readPageData, pureRendering=$pureRendering, openTime=$openTime, closeTime=$closeTime, book=$book"
        }
    }

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
            val book = it.openBook()
            val openTime = timeDelta(time)

            val pureRendering = mutableListOf<Long>()
            val pageInfo = mutableListOf<Long>()

            val readData = (0..20).map { pageNum ->
                bitmap.eraseColor(Color.TRANSPARENT)
                val page = book.getOrCreatePageAdapter(pageNum)
                pageInfo.add(
                    time {
                        page.getPageDimension()
                    }
                )
                val goto = time()
                page.readPageDataForRendering()
                val res = timeDelta(goto)
                pureRendering.add(
                    time {
                        page.renderPage(bitmap, 1.0, 0, 0, bitmap.width, bitmap.height, 0, 0)
                    }
                )
                page.destroy()
                res
            }

            val closeTime = time {
                book.destroy()
            }

            Info(
                openTime,
                DataList(pageInfo),
                DataList(readData),
                DataList(pureRendering),
                closeTime,
                it
            )
        }

        println(infos.joinToString("\n"))
    }

    private fun time() = System.currentTimeMillis()

    private fun timeDelta(start: Long) = System.currentTimeMillis() - start

    private inline fun time(block: () -> Unit): Long {
        val start = time()
        block()
        return timeDelta(start)
    }

}