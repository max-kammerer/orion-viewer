package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.graphics.Point
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.prefs.GlobalOptions.OPEN_AS_TEMP_BOOK
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_HEIGHT
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_WIDTH
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

private val deviceSize = Point(300, 350) //to split page on two screen - page size is 663x886

@RunWith(Parameterized::class)
class RenderingAndNavigationTest(private val book: BookDescription) : InstrumentationTestCase(book.toOpenIntent(), additionalParams = {
    intent ->
    intent.putExtra(TEST_SCREEN_WIDTH, deviceSize.x)
    intent.putExtra(TEST_SCREEN_HEIGHT, deviceSize.y)
    intent.putExtra(OPEN_AS_TEMP_BOOK, true)
}) {

   companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test simple navigation in {0}")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.values().map { arrayOf(it) }
            //return arrayOf(arrayOf(BookDescription.values().first())).asIterable()
        }
    }


    @Test
    fun testProperPages() {
        doTestProperPages()
    }

    private fun doTestProperPages() {
        val controller = prepareEngine()
        val screens = 21

        assertEquals(book.pageCount, controller.pageCount)
        assertEquals(0, controller.currentPage)

        val nextPageList = arrayListOf<IntArray>()
        //TODO
        repeat(screens) {
            val latch = CountDownLatch(1)
            activityScenarioRule.scenario.onActivity { activity ->
                controller.drawNext()
                val page = controller.currentPage

                controller.pageLayoutManager.pageListener = object : PageInitListener {
                    var counter = 0
                    override fun onPageInited(pageView: PageView) {
                        val pinfo = controller.pageLayoutManager.currentPageLayout()!!
                        controller.drawPage(pinfo.pageNumber, pinfo.x.offset, pinfo.y.offset) {
                            processBitmap(nextPageList, it as Bitmap)
                            println("Process bitmap ${pageView.pageNum}")
                            controller.pageLayoutManager.pageListener = null
                            latch.countDown()
                            assertEquals(1, ++counter)
                            assertEquals(page, pageView.pageNum)
                        }
                    }
                }
            }
            latch.await()
        }

        val prevPageList = arrayListOf<IntArray>()
        repeat(screens) {
            val latch = CountDownLatch(1)
            activityScenarioRule.scenario.onActivity { activity ->
                controller.drawPrev()
                val page = controller.currentPage

                controller.pageLayoutManager.pageListener = object : PageInitListener {
                    var counter = 0
                    override fun onPageInited(pageView: PageView) {
                        val pinfo = controller.pageLayoutManager.currentPageLayout()!!
                        controller.drawPage(pinfo.pageNumber, pinfo.x.offset, pinfo.y.offset) {
                            processBitmap(prevPageList, it as Bitmap)
                            println("Process bitmap ${pageView.pageNum}")
                            controller.pageLayoutManager.pageListener = null
                            latch.countDown()
                            assertEquals(1, ++counter)
                            assertEquals(page, pageView.pageNum)
                        }
                    }
                }
            }
            latch.await()
        }


        assertEquals(nextPageList.size, screens)
        assertEquals(prevPageList.size, screens)

        nextPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse(
                "Next screens $index and ${index + 1} are equals: ${left.joinToString()}",
                left.contentEquals(right)
            )
        }

        prevPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse(
                "Prev screens $index and ${index + 1} are equals: ${left.joinToString()}",
                left.contentEquals(right)
            )
        }

        nextPageList.zip(prevPageList.reversed()).forEachIndexed() { index, (next, prev) ->
            assertArrayEquals("fail on $index", next, prev)
        }
    }

    private fun processBitmap(list: MutableList<IntArray>, bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        list.add(pixels)
    }

    private fun prepareEngine(): Controller {
        val ref = AtomicReference<Controller>()
        activityScenarioRule.scenario.onActivity { activity ->
            val controller = activity.controller!!
            controller.pageLayoutManager.isSinglePageMode = true
            ref.set(controller)
        }
        return ref.get()
    }
}