package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.prefs.GlobalOptions.OPEN_AS_TEMP_BOOK
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_HEIGHT
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_WIDTH
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.MANUAL_DEBUG
import universe.constellation.orion.viewer.test.framework.compareBitmaps
import universe.constellation.orion.viewer.test.framework.dumpBitmap
import java.nio.IntBuffer


private val deviceSize = Point(300, 350) //to split page on two screen - page size is 663x886

@Ignore
@RunWith(Parameterized::class)
class RenderingAndNavigationTest(private val book: BookDescription) : InstrumentationTestCase(book.toOpenIntent(), additionalParams = {
    intent ->
    intent.putExtra(TEST_SCREEN_WIDTH, deviceSize.x)
    intent.putExtra(TEST_SCREEN_HEIGHT, deviceSize.y)
    intent.putExtra(OPEN_AS_TEMP_BOOK, true)
}) {

   companion object {
        const val SCREENS = 21

        @JvmStatic
        @Parameterized.Parameters(name = "Test simple navigation in {0}")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.testData()
        }
    }

    @Volatile
    lateinit var canvas: Canvas
    @Volatile
    lateinit var bitmap: Bitmap

    @Volatile
    lateinit var controller: Controller

    @Test
    fun testProperPages() {
        doTestProperPages()
    }

    private fun doTestProperPages() {
        prepareEngine()

        assertEquals(deviceSize.x, canvas.width)
        assertEquals(deviceSize.y, canvas.height)
        assertEquals(deviceSize.x, bitmap.width)
        assertEquals(deviceSize.y, bitmap.height)

        assertEquals("Check page count:", book.pageCount, controller.pageCount)
        assertEquals(0, controller.currentPage)

        val nextPageList = arrayListOf<IntArray>()
        val nextPageRects = arrayListOf<Rect>()
        repeat(SCREENS) {
            lateinit var page: Deferred<PageView?>
            activityScenarioRule.scenario.onActivity { activity ->
                page = controller.drawNext() ?: error("null on ${controller.pageLayoutManager.currentPageLayout()}")
            }
            runBlocking {
                val pageView = page.await()!!
                nextPageRects.add(pageView.occupiedVisiblePartInNewRect()!!)
                flushAndProcessBitmap("next", nextPageList)
            }
        }

        val prevPageList = arrayListOf<IntArray>()
        val prevPageRects = arrayListOf<Rect>()
        repeat(SCREENS) {
            lateinit var page: Deferred<PageView?>
            activityScenarioRule.scenario.onActivity {
                page = controller.drawPrev()!!
            }
            runBlocking {
                val pageView = page.await()!!
                prevPageRects.add(pageView.occupiedVisiblePartInNewRect()!!)
                flushAndProcessBitmap("prev", prevPageList)
            }
        }

        assertEquals(nextPageList.size, SCREENS)
        assertEquals(prevPageList.size, SCREENS)

        nextPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            val contentEquals = left.contentEquals(right)
            if (contentEquals) {
                dump(left, right, index, index+1)
            }
            assertFalse(
                "Next screens $index and ${index + 1} are equals: see dump",
                contentEquals
            )
        }

        prevPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            val contentEquals = left.contentEquals(right)
            if (contentEquals) {
                dump(left, right, index, index+1)
            }
            assertFalse(
                "Prev screens $index and ${index + 1} are equals: see dump",
                left.contentEquals(right)
            )
        }

        nextPageRects.dropLast(1).reversed().zip(prevPageRects.dropLast(1)).forEachIndexed { index, (next, prev) ->
            assertEquals("fail on ${SCREENS - index - 2} and $index", next, prev)
        }

        nextPageList.dropLast(1).reversed().zip(prevPageList.dropLast(1)).forEachIndexed { index, (next, prev) ->
            compareBitmaps(next, prev, bitmap.width, "Fail on ${SCREENS - index - 2} and $index") {
                dump(next, prev, SCREENS - index - 2, index)
            }
        }
    }

    private fun flushAndProcessBitmap(
        suffix: String,
        list: ArrayList<IntArray>
    ) {
        activityScenarioRule.scenario.onActivity {
            bitmap.eraseColor(0)
            it.view.draw(canvas)
            processBitmap(list, bitmap)
            if (MANUAL_DEBUG) {
                dump(
                    list.last(),
                    list.size - 1,
                    suffix
                )
            }
        }
    }

    private fun processBitmap(list: MutableList<IntArray>, bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        list.add(pixels)
    }

    private fun prepareEngine() {
        activityScenarioRule.scenario.onActivity { activity ->
            controller = activity.controller!!
            controller.pageLayoutManager.isSinglePageMode = true
            bitmap = Bitmap.createBitmap(
                activity.view.width,
                activity.view.height,
                Bitmap.Config.ARGB_8888
            )
            canvas = Canvas(bitmap)
        }
    }

    private fun dump(data1: IntArray, data2: IntArray, index1: Int, index2: Int): String {
        dump(data1, index1)
        dump(data2, index2)
        return ""
    }

    private fun dump(data: IntArray, index: Int, suffix: String ="") {
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(data))

        dumpBitmap("rant","$suffix${String.format("%02d",index)}", bitmap)
    }
}
