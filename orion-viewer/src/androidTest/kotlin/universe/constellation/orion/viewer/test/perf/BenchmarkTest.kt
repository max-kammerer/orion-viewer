package universe.constellation.orion.viewer.test.perf

import android.graphics.Color
import com.artifex.mupdf.fitz.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

const val WIDTH = 800
const val HEIGHT = 1024

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
        val bigPartRendering: DataList,
        val partRendering: DataList,
        val closeTime: Long,
        val book: BookFile
    ) {
        override fun toString(): String {
            return "book=$book, openTime=$openTime, closeTime=$closeTime, \npageInfo=$pageInfoAvg, readPageData=$readPageData, \npure=$pureRendering, \nbigg=$bigPartRendering, \npart=$partRendering"
        }
    }

    companion object {
        private val BITMAP_CACHE = BitmapCache(20)
    }

    private val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
    private val bitmapFull = FlexibleBitmap(WIDTH, HEIGHT, WIDTH, HEIGHT)
    private val bitmap4Parts = FlexibleBitmap(WIDTH, HEIGHT, WIDTH / 2, HEIGHT / 2)

    @Before
    fun setUp() {
        DjvuDocument.Companion
        Context.init()
    }

    @After
    fun tearDown() {
        bitmap.recycle()
        BITMAP_CACHE.free()
    }

    @Test
    fun benchmark() {
        val infos = BookFile.testEntriesWithCustoms().map {
            val time = time()
            val book = it.openBook()
            val openTime = timeDelta(time)

            val pureRendering = mutableListOf<Long>()
            val pageInfo = mutableListOf<Long>()
            val partRendering = mutableListOf<Long>()
            val bigPartRendering = mutableListOf<Long>()

            val readData = (0..20).map { pageNum ->
                bitmap.eraseColor(Color.TRANSPARENT)
                val page = book.getOrCreatePageAdapter(pageNum)
                pageInfo.add(
                    time {
                        page.getPageDimension()
                    }
                )
                val res = time {
                    page.readPageDataForRendering()
                }

                pureRendering.add(
                    time {
                        page.renderPage(bitmap, 1.0, 0, 0, bitmap.width, bitmap.height, 0, 0)
                    }
                )

                bitmapFull.enableAll(BITMAP_CACHE)
                bigPartRendering.add(
                    time {
                        bitmapFull.renderFull(1.0, page)
                    }
                )
                bitmapFull.disableAll(BITMAP_CACHE)

                bitmap4Parts.enableAll(BITMAP_CACHE)
                partRendering.add(
                    time {
                        bitmap4Parts.renderFull(1.0, page)
                    }
                )
                bitmap4Parts.disableAll(BITMAP_CACHE)


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
                DataList(bigPartRendering),
                DataList(partRendering),
                closeTime,
                it
            )
        }

        println(infos.joinToString("\n\n"))
    }

    private fun time() = System.currentTimeMillis()

    private fun timeDelta(start: Long) = System.currentTimeMillis() - start

    private inline fun time(block: () -> Unit): Long {
        val start = time()
        block()
        return timeDelta(start)
    }

}