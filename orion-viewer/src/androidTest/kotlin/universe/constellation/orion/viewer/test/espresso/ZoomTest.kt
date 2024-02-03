package universe.constellation.orion.viewer.test.espresso

import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase


@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ZoomTest: InstrumentationTestCase(BookDescription.SICP.toOpenIntent()) {

    @Test
    fun testZoomChange() {
        openZoom()
        onView(withId(R.id.zoom_picker_seeker)).perform(setProgress { it / 3 })
        applyZoom()
        Thread.sleep(1000)
        onView(withId(R.id.view)).perform(swipeUp())

        Thread.sleep(1000)
        openZoom()
        onView(withId(R.id.zoom_picker_seeker)).perform(setProgress { it * 2 })
        applyZoom()

        Thread.sleep(1000)
        onView(withId(R.id.view)).perform(swipeUp())
    }

    private fun applyZoom() {
        onView(withId(R.id.zoom_preview)).perform(click())
        onView(withId(R.id.zoom_picker_close)).perform(click())
    }

    private fun openZoom() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText("Zoom")).perform(click())
    }


}

private fun setProgress(transform: (Int) -> Int): ViewAction {
    return object : ViewAction {
        override fun perform(uiController: UiController?, view: View) {
            val seekBar = view as SeekBar
            val transform1 = transform(seekBar.progress)
            seekBar.progress = transform1
        }

        override fun getDescription(): String {
            return "Set a progress on a SeekBar"
        }

        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(SeekBar::class.java)
        }
    }
}