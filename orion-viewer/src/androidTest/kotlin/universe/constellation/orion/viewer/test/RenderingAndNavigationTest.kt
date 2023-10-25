package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.graphics.Point
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.document.DocumentWithCaching
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.initalizer
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.SingleThreadRenderer
import universe.constellation.orion.viewer.view.Scene
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

@RunWith(Parameterized::class)
class RenderingAndNavigationTest(book: BookDescription) : InstrumentationTestCase(book.toOpenIntent()) {

   companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test simple navigation in {0}")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.values().map { arrayOf(it) }
        }
    }

    class MyView(private val imageView: OrionImageListener) : Scene, OrionBookListener {

        @Volatile
        var data: Bitmap? = null

        override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
            imageView.onNewImage(bitmap, info, latch)
            this.data = bitmap
        }

        override fun onNewBook(title: String?, pageCount: Int) {

        }
    }

    private val deviceSize = Point(300, 350) //to split page on two screen - page size is 663x886

    private lateinit var view: MyView

    @Before
    fun setUp() {
        val ref = AtomicReference<OrionImageListener>()
        activityScenarioRule.scenario.onActivity {
            ref.set(it.view)
        }
        view = MyView(ref.get())
    }

    @Test
    fun testProperPages() {
        doTestProperPages()
    }

    private fun doTestProperPages() {
        val controller = prepareEngine()
        val screens = 21

        val nexts = arrayListOf<IntArray>()
        repeat(screens) {
            processBitmap(nexts) { controller.drawNext() }
        }

        controller.drawNext()

        val prevs = arrayListOf<IntArray>()
        repeat(screens) {
            processBitmap(prevs) { controller.drawPrev() }
        }

        Assert.assertEquals(prevs.size, screens)
        Assert.assertEquals(nexts.size, screens)

        nexts.zipWithNext().forEachIndexed { index, (left, right) ->
            Assert.assertFalse("fail on $index", left.contentEquals(right))
        }

        prevs.zipWithNext().forEachIndexed { index, (left, right) ->
            Assert.assertFalse("fail on $index", left.contentEquals(right))
        }

        nexts.zip(prevs.reversed()).forEachIndexed() { index, (next, prev) ->
            Assert.assertArrayEquals("fail on $index", next, prev)
        }
    }

    private fun processBitmap(list: MutableList<IntArray>, drawer: () -> Unit) {
        drawer()
        val bitmap = view.data!!
        Assert.assertNotNull(bitmap)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        list.add(pixels)
    }

    private fun prepareEngine(): Controller {
        lateinit var controller: Controller
        activityScenarioRule.scenario.onActivity { activity ->
            val document = activity.controller!!.document as DocumentWithCaching
            val layoutStrategy: LayoutStrategy = SimpleLayoutStrategy.create(document)
            val renderer = SingleThreadRenderer(activity, view, layoutStrategy, document)
            controller = Controller(activity, document, layoutStrategy, renderer)


            val lastPageInfo =
                LastPageInfo.loadBookParameters(activity, "123", initalizer(activity.globalOptions))
            controller.changeOrinatation(lastPageInfo.screenOrientation)
            controller.init(lastPageInfo, deviceSize)

            //getSubscriptionManager()?.sendDocOpenedNotification(controller)
            activity.view.setDimensionAware(controller)
        }
        return controller
    }
}