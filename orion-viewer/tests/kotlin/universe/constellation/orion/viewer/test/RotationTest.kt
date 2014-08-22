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

        runTestOnUiThread { getActivity().changeOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }



        Thread.sleep(2000)

        val width2 = view.getWidth()
        val height2 = view.getHeight()

        Assert.assertTrue(width != width2)
        Assert.assertTrue(height != height2)
    }
}