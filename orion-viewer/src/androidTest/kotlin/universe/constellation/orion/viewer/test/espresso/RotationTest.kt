package universe.constellation.orion.viewer.test.espresso

import android.content.pm.ActivityInfo
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.view.OrionDrawScene

class RotationTest : BaseEspressoTest(BookDescription.SICP) {

    @Test
    @SdkSuppress(minSdkVersion = 21)
    fun testScreenRotationAndViewSize() {
        assertTrue(device.isNaturalOrientation)

        val (width, height) = getViewSize()

        var orientation: Int = Int.MIN_VALUE
        activityScenarioRule.scenario.onActivity {
            orientation = it.resources!!.configuration.orientation
            it.controller!!.changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE")
        }
        device.waitForIdle()
        assertFalse(device.isNaturalOrientation)

        var newOrientation: Int = Int.MAX_VALUE
        activityScenarioRule.scenario.onActivity {
            newOrientation = it.resources!!.configuration.orientation
        }

        assertNotEquals("Orientation not changed: $orientation", orientation, newOrientation)
        val (rotatedWidth, rotatedHeight)  = getViewSize()
        assertNotEquals("Widths should not be equal, original orientation: $orientation", width, rotatedWidth)
        assertNotEquals("Heights should not be equal, original orientation: $orientation", height, rotatedHeight)

        activityScenarioRule.scenario.onActivity {
            it.controller!!.changeOrinatation("PORTRAIT")
        }
        device.waitForIdle()
        assertTrue(device.isNaturalOrientation)
    }

    private fun getViewSize(): Pair<Int, Int> {
        lateinit var view: OrionDrawScene
        activityScenarioRule.scenario.onActivity {
            view = it.view
        }

        val width = view.sceneWidth
        val height = view.sceneHeight

        assertEquals(width, device.displayWidth)
        assertTrue(height > 0 && height <= device.displayHeight)
        return width to height
    }

    @After
    fun afterTest() {
        device.setOrientationNatural()
    }
}