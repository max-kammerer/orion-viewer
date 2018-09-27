package universe.constellation.orion.viewer.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import junit.framework.Assert
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.initalizer
import universe.constellation.orion.viewer.test.framework.ActivityBaseTest
import universe.constellation.orion.viewer.test.framework.SingleThreadRenderer
import universe.constellation.orion.viewer.test.framework.TestUtil
import universe.constellation.orion.viewer.view.Scene
import java.util.*
import java.util.concurrent.CountDownLatch

class RenderingAndNavigationTest : ActivityBaseTest() {

    class MyView(private val imageView: OrionImageListener) : Scene, OrionBookListener {

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

    override fun setUp() {
        super.setUp()
        view = MyView(activity.view)
    }

    fun testProperPagesSkip() {
        doTestProperPages(TestUtil.SICP)
    }

    fun testProperPagesAlice() {
        doTestProperPages(TestUtil.ALICE)
    }

    private fun doTestProperPages(book: String) {
        val controller = prepareEngine(book)
        val screens = 21

        val nexts = arrayListOf<IntArray>()
        (1..screens).forEach {
            processBitmap(nexts) { controller.drawNext() }
        }

        controller.drawNext()

        val prevs = arrayListOf<IntArray>()
        (1..screens).forEach {
            processBitmap(prevs) { controller.drawPrev() }
        }

        (1..screens - 2).forEach { i ->
            Assert.assertFalse("fail on $i", Arrays.equals(nexts[i], nexts[i + 1]))
            Assert.assertFalse("fail on $i", Arrays.equals(prevs[i], prevs[i + 1]))
        }

        (screens - 1 downTo 1).forEach { i ->
            println("$i")
            Assert.assertTrue("fail on $i", Arrays.equals(nexts[i], prevs[prevs.lastIndex - i]))
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

    private fun prepareEngine(book: String): Controller {
        val doc = openTestBook(book)

        val layoutStrategy: LayoutStrategy = SimpleLayoutStrategy.create(doc)
        val renderer = SingleThreadRenderer(activity, view, layoutStrategy, doc)
        val controller = Controller(activity, doc, layoutStrategy, renderer)


        val lastPageInfo = LastPageInfo.loadBookParameters(activity, "123", initalizer(activity.globalOptions))
        controller.changeOrinatation(lastPageInfo.screenOrientation)
        controller.init(lastPageInfo, deviceSize)

        //getSubscriptionManager()?.sendDocOpenedNotification(controller)
        activity.view.setDimensionAware(controller)
        return controller
    }


    override fun getOrionTestContext(): Context {
        return instrumentation!!.context!!
    }
}