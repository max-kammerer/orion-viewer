package universe.constellation.orion.viewer.test.espresso

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BaseUITest
import universe.constellation.orion.viewer.test.framework.openOrionIntent
import java.util.concurrent.atomic.AtomicReference

class NoBookNoStartTapScreen : BaseUITest(openOrionIntent {
    putExtra(GlobalOptions.SHOW_TAP_HELP, true)
}, true) {
    @Test
    fun testStartScreenAbsent() {
        onView(withId(R.id.tap_help_close)).check(doesNotExist())
        assertTrue(globalOptions.isShowTapHelp)
    }
}

class BookWithStartTapScreen :
    BaseViewerActivityTest(
        BookDescription.SICP,
        BookDescription.SICP.toOpenIntent {
            putExtra(GlobalOptions.SHOW_TAP_HELP, true)
        }) {

    @Test
    fun testStartScreen() {
        assertTrue(globalOptions.isShowTapHelp)
        onView(withId(R.id.tap_help_close)).check(matches(isDisplayed()))
        checkSizeAndPosition()

        onView(withId(R.id.tap_help_close)).perform(click())
        onView(withId(R.id.tap_help_close)).check(doesNotExist())
        assertFalse(globalOptions.isShowTapHelp)
    }

    private fun checkSizeAndPosition() {
        val viewRect = AtomicReference<Rect>()
        val displayRect = AtomicReference<Rect>()
        val loc = AtomicReference<IntArray>()
        activityScenarioRule.scenario.onActivity { it ->
            val r = Rect()
            (it.view as View).getLocalVisibleRect(r)
            viewRect.set(r)
            val l = IntArray(2)
            (it.view as View).getLocationOnScreen(l)
            loc.set(l)

            val displayMetrics = it.resources.displayMetrics
            displayRect.set(Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels))
            //windowRect.set(it.windowManager.currentWindowMetrics.bounds)
        }

        onView(withId(R.id.tap_help_close)).check { view, noViewFoundException ->
            val dialogRect = Rect()
            val rootView = view.rootView
            rootView.getLocalVisibleRect(dialogRect)
            val expectedRect = viewRect.get()
            assertEquals(expectedRect, dialogRect)

            val l = IntArray(2)
            rootView.getLocationOnScreen(l)
            assertArrayEquals(loc.get(), l)

            val screen = displayRect.get()
            assert(expectedRect.width() * expectedRect.height() >= 0.8 * screen.width() * screen.height())
        }
    }
}