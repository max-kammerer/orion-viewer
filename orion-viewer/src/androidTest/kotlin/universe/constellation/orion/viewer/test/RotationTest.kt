package universe.constellation.orion.viewer.test

import android.content.pm.ActivityInfo
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.test.espresso.BaseOrionActivityTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.view.OrionDrawScene

class RotationTest : BaseOrionActivityTest(BookDescription.SICP) {

    @Test
    @SdkSuppress(minSdkVersion = 21)
    fun testRotation() {
        lateinit var view: OrionDrawScene
        activityScenarioRule.scenario.onActivity {
            view = it.view
        }

        val width = view.sceneWidth
        val height = view.sceneHeight

        Assert.assertTrue(width != 0)
        Assert.assertTrue(height != 0)

        var orientation: Int = Int.MIN_VALUE
        activityScenarioRule.scenario.onActivity {
            orientation = it.resources!!.configuration.orientation
            it.controller!!.changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE")
        }

        Thread.sleep(1000)

        try {
            var newOrientation: Int = Int.MAX_VALUE
            activityScenarioRule.scenario.onActivity {
                newOrientation = it.resources!!.configuration.orientation
            }

            Assert.assertNotEquals("Orientation not changed: $orientation", orientation, newOrientation)
            val width2 = view.sceneWidth
            val height2 = view.sceneHeight
            Assert.assertTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
            Assert.assertTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
        } finally {
            activityScenarioRule.scenario.onActivity {
                it.controller!!.changeOrinatation("PORTRAIT")
            }
            Thread.sleep(1000)
            activityScenarioRule.scenario.onActivity {
                it.finishActivity(0)
            }
        }
    }
}