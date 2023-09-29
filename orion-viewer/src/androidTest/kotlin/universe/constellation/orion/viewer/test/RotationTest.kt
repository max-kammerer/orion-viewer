package universe.constellation.orion.viewer.test

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.TestUtil
import java.util.concurrent.CountDownLatch

class RotationTest : InstrumentationTestCase() {

    @Test
    @Ignore
    fun testRotation() {
        val file = extractFileFromTestData(TestUtil.SICP)
        val intent = Intent()
        intent.data = Uri.fromFile(file)
        startActivityWithBook(intent)

        val view = activity.view
        val width = view.sceneWidth
        val height = view.sceneHeight

        Assert.assertTrue(width != 0)
        Assert.assertTrue(height != 0)
        val orientation = activity.resources!!.configuration.orientation

        mActivityRule.runOnUiThread {
            getController().changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE")
        }

        Thread.sleep(1000)

        try {
            Assert.assertTrue("Orientation not changed: $orientation", orientation != activity.resources!!.configuration.orientation)
            val width2 = view.sceneWidth
            val height2 = view.sceneHeight
            Assert.assertTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
            Assert.assertTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
        } finally {
            mActivityRule.runOnUiThread {
                getController().changeOrinatation("PORTRAIT")
            }
            Thread.sleep(1000)
            mActivityRule.finishActivity()
        }
    }
}