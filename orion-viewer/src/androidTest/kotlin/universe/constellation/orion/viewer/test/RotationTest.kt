package universe.constellation.orion.viewer.test

import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.OrionViewerActivity
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
        val (width, height) = onActivity {
            it.view.sceneWidth to it.view.sceneHeight
        }

        checkTrue("Width is empty", width != 0)
        checkTrue("Height is empty", height != 0)

        var orientation: Int = Int.MIN_VALUE
        onActivity {
            orientation = it.resources!!.configuration.orientation
            it.controller!!.changeOrinatation(if (orientation == Configuration.ORIENTATION_LANDSCAPE) "PORTRAIT" else "LANDSCAPE")
        }

        try {
            /* The rotation settles some time after the request: the scene is resized and the
               activity's configuration updated in separate steps, neither by the time the device
               is idle for the first time. */
            val (newOrientation, width2, height2) = awaitOnActivity({ it.first != orientation && it.second != width }) {
                Triple(it.resources!!.configuration.orientation, it.view.sceneWidth, it.view.sceneHeight)
            }

            checkNotEquals("Orientation not changed: $orientation", orientation, newOrientation)
            checkTrue("w1: $width, w2: $width2, original orientation: $orientation", width != width2)
            checkTrue("h1: $height, h2: $$height2, original orientation: $orientation", height != height2)
        } finally {
            onActivity {
                it.controller!!.changeOrinatation("PORTRAIT")
            }
            device.waitForIdle()
        }
    }

    /** Reads [value] on the activity until it satisfies [done] or the timeout is over; the last one read is returned. */
    private fun <T : Any> awaitOnActivity(done: (T) -> Boolean, value: (OrionViewerActivity) -> T): T {
        val deadline = SystemClock.uptimeMillis() + ROTATION_TIMEOUT
        while (true) {
            device.waitForIdle()
            val current = onActivity(value)
            if (done(current) || SystemClock.uptimeMillis() > deadline) return current
            Thread.sleep(100)
        }
    }

    private companion object {
        const val ROTATION_TIMEOUT = 10_000L
    }
}