package universe.constellation.orion.viewer.test

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import junit.framework.Assert
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

        val view = activity.view!!
        val width = view.width
        val height = view.height

        Assert.assertTrue(width != 0)
        Assert.assertTrue(height != 0)
        //OptionActions.FULL_SCREEN.doAction(getActivity(), true, false)
        val orientation = activity.resources!!.configuration.orientation;

        val latch = CountDownLatch(1);
        runTestOnUiThread {
            getController().changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE");
            latch.countDown();
        }
        latch.await();
        activity.finish()

        Assert.assertTrue("Orintation not changed: $orientation", orientation != activity.resources!!.configuration.orientation)

        val width2 = view.width
        val height2 = view.height
        Assert.assertTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
        Assert.assertTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
    }
}