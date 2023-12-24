package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.graphics.Point
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.initalizer
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.openTestBook
import universe.constellation.orion.viewer.view.OrionDrawScene
import java.util.concurrent.atomic.AtomicReference

@RunWith(Parameterized::class)
class RenderingAndNavigationTest(private val book: BookDescription) : InstrumentationTestCase(book.toOpenIntent()) {

   companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test simple navigation in {0}")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.values().map { arrayOf(it) }
        }
    }

    private val deviceSize = Point(300, 350) //to split page on two screen - page size is 663x886

    @Test
    fun testProperPages() {
        doTestProperPages()
    }

    private fun doTestProperPages() {
        val controller = prepareEngine()
        val screens = 21

        assertEquals(0, controller.currentPage)
        assertEquals(book.pageCount, controller.pageCount)

        val nexts = arrayListOf<IntArray>()
        repeat(screens) {
            processBitmap(nexts) { controller.drawNext() }
        }

        controller.drawNext()

        val prevs = arrayListOf<IntArray>()
        repeat(screens) {
            processBitmap(prevs) { controller.drawPrev() }
        }

        assertEquals(prevs.size, screens)
        assertEquals(nexts.size, screens)

        nexts.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse("Screens $index and ${index+1} are equals: ${left.joinToString()}", left.contentEquals(right))
        }

        prevs.zipWithNext().forEachIndexed { index, (left, right) ->
            assertFalse("Screens $index and ${index+1} are equals: ${left.joinToString()}", left.contentEquals(right))
        }

        nexts.zip(prevs.reversed()).forEachIndexed() { index, (next, prev) ->
            assertArrayEquals("fail on $index", next, prev)
        }
    }

    private fun processBitmap(list: MutableList<IntArray>, drawer: () -> Unit) {
        drawer()
        //TODO: rework
        val bitmapRef = AtomicReference<Bitmap>()
        while (bitmapRef.get() == null) {
            activityScenarioRule.scenario.onActivity {
                bitmapRef.set((it.fullScene.drawView as OrionDrawScene).pageView?.bitmap)
            }
            Thread.sleep(50)
        }
        val bitmap = bitmapRef.get()
        assertNotNull(bitmap)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        list.add(pixels)
    }

    private fun prepareEngine(): Controller {
        val ref = AtomicReference<Controller>()
        activityScenarioRule.scenario.onActivity { activity ->
            val document = openTestBook(book)
            val layoutStrategy: LayoutStrategy = SimpleLayoutStrategy.create(document)
            val controller = Controller(activity, document, layoutStrategy)

            val lastPageInfo =
                LastPageInfo.loadBookParameters(activity, "123", initalizer(activity.globalOptions))
            controller.changeOrinatation(lastPageInfo.screenOrientation)
            controller.init(lastPageInfo, deviceSize)

            //getSubscriptionManager()?.sendDocOpenedNotification(controller)
            activity.view.setDimensionAware(controller)
            ref.set(controller)
        }
        return ref.get()
    }
}