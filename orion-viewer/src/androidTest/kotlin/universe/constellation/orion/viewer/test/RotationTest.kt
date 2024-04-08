package universe.constellation.orion.viewer.test

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.test.espresso.BaseViewerActivityTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.checkNotEquals
import universe.constellation.orion.viewer.test.framework.checkTrue
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.view.OrionDrawScene

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class RotationTest : BaseViewerActivityTest(BookDescription.SICP) {

    @Test
    fun testRotation() {
        lateinit var view: OrionDrawScene
        onActivity {
            view = it.view
        }

        val width = view.sceneWidth
        val height = view.sceneHeight

        checkTrue("Width is empty", width != 0)
        checkTrue("Height is empty", height != 0)

        var orientation: Int = Int.MIN_VALUE
        onActivity {
            orientation = it.resources!!.configuration.orientation
            it.controller!!.changeOrinatation(if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE")
        }

        device.waitForIdle()

        try {
            val newOrientation = onActivity {
                it.resources!!.configuration.orientation
            }

            checkNotEquals("Orientation not changed: $orientation", orientation, newOrientation)
            val width2 = view.sceneWidth
            val height2 = view.sceneHeight
            checkTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
            checkTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
        } finally {
            onActivity {
                it.controller!!.changeOrinatation("PORTRAIT")
            }
            device.waitForIdle()
        }
    }
}