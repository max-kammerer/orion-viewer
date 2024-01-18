package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.graphics.Point
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_HEIGHT
import universe.constellation.orion.viewer.prefs.GlobalOptions.TEST_SCREEN_WIDTH
import universe.constellation.orion.viewer.prefs.initalizer
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
}) {

   companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test simple navigation in {0}")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.values().map { arrayOf(it) }
            //return arrayOf(arrayOf(BookDescription.entries.first())).asIterable()
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

                controller.pageLayoutManager.pageListener = object : PageInitListener {
                    override fun onPageInited(pageView: PageView) {
                        controller.drawPage {
                            processBitmap(nextPageList, it as Bitmap)
                            controller.pageLayoutManager.pageListener = null
                            latch.countDown()
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
                controller.pageLayoutManager.pageListener = object : PageInitListener {
                    override fun onPageInited(pageView: PageView) {
                        controller.drawPage {
                            processBitmap(prevPageList, it as Bitmap)
                            latch.countDown()
                            controller.pageLayoutManager.pageListener = null
                        }
                    }
                }
            }
            latch.await()
        }


        assertEquals(nextPageList.size, screens)
        assertEquals(prevPageList.size, screens)

        nextPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse("Screens $index and ${index+1} are equals: ${left.joinToString()}", left.contentEquals(right))
        }

        prevPageList.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse("Screens $index and ${index+1} are equals: ${left.joinToString()}", left.contentEquals(right))
        }

        nextPageList.zip(prevPageList.reversed()).forEachIndexed() { index, (next, prev) ->
            assertArrayEquals("fail on $index", next, prev)
        }
    }

    private fun processBitmap(list: MutableList<IntArray>, bitmap: Bitmap) {
        assertNotNull(bitmap)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        list.add(pixels)
    }

    private fun prepareEngine(): Controller {
        val ref = AtomicReference<Controller>()
        activityScenarioRule.scenario.onActivity { activity ->
//            val document = openTestBook(book)
//            val layoutStrategy: LayoutStrategy = SimpleLayoutStrategy.create(document)
//            val controller = Controller(activity, document, layoutStrategy)
            val lastPageInfo =
                LastPageInfo.loadBookParameters(activity, "123", initalizer(activity.globalOptions))
//            controller.changeOrinatation(lastPageInfo.screenOrientation)

            //getSubscriptionManager()?.sendDocOpenedNotification(controller)
            val controller = activity.controller!!
            controller.init(lastPageInfo, deviceSize)
            controller.pageLayoutManager.isSinglePageMode = true
            controller.forcePageRecreation()
            ref.set(controller)
        }
        return ref.get()
    }
}