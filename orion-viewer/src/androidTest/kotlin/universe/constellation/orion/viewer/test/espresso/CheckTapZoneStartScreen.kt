package universe.constellation.orion.viewer.test.espresso

import android.graphics.Rect
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.createTestViewerIntent
import universe.constellation.orion.viewer.test.framework.onActivity

class NoBookNoStartTapScreen : BaseTestWithActivity(createTestViewerIntent {
    putExtra(GlobalOptions.SHOW_TAP_HELP, true)
}) {
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
        onView(withId(R.id.tap_help_close)).check(matches(isDisplayed()))
        checkSizeAndPosition()

        onView(withId(R.id.tap_help_close)).perform(click())
        onView(withId(R.id.tap_help_close)).check(doesNotExist())
        assertFalse(globalOptions.isShowTapHelp)
    }

    private fun checkSizeAndPosition() {
        onActivity { it ->
            val findFragmentByTag = it.supportFragmentManager.findFragmentByTag("TAP_HELP")
            val viewerRect = Rect()
            val mainFrame = it.findViewById<View>(R.id.main_frame)
            mainFrame.getGlobalVisibleRect(viewerRect)

            val displayMetrics = it.resources.displayMetrics
            val displayRect = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

            val dialogRect = Rect()
            val tapHelpContent = (findFragmentByTag!! as DialogFragment).view!!
            tapHelpContent.getGlobalVisibleRect(dialogRect)

            assert(dialogRect.width() * dialogRect.height() >= 0.9 * viewerRect.width() * viewerRect.height())
            assert(dialogRect.width() * dialogRect.height() >= 0.9 * displayRect.width() * displayRect.height())
        }
    }
}