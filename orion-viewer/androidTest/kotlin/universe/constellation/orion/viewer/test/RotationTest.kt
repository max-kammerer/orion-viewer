package universe.constellation.orion.viewer.test

import android.view.ViewGroup.LayoutParams
import universe.constellation.orion.viewer.OptionActions
import junit.framework.Assert
import android.view.WindowManager
import android.graphics.PixelFormat
import android.content.Intent
import universe.constellation.orion.viewer.OrionViewerActivity
import android.net.Uri
import android.test.ActivityInstrumentationTestCase2
import android.content.pm.ActivityInfo
import android.test.UiThreadTest
import java.util.concurrent.CountDownLatch

/**
 * User: mike
 * Date: 21.10.13
 * Time: 7:17
 */
class RotationTest() : InstrumentationTestCase() {

    private val width = 300;
    private val height = 400;


    fun testRotation() {
        val file = extractFileFromTestData(TestUtil.SICP)
        val intent = Intent();
        intent.setData(Uri.fromFile(file))
        setActivityIntent(intent)

        val view = getActivity().getView()!!
        val width = view.getWidth()
        val height = view.getHeight()

        Assert.assertTrue(width != 0)
        Assert.assertTrue(height != 0)
        //OptionActions.FULL_SCREEN.doAction(getActivity(), true, false)
        val orientation = getActivity().getResources()!!.getConfiguration().orientation;

        val latch = CountDownLatch(1);
        runTestOnUiThread {
            getController().changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE");
            latch.countDown();
        }
        latch.await();
        getActivity().finish()

        Assert.assertTrue("Orintation not changed: $orientation", orientation != getActivity().getResources()!!.getConfiguration().orientation)

        val width2 = view.getWidth()
        val height2 = view.getHeight()
        Assert.assertTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
        Assert.assertTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
    }
}