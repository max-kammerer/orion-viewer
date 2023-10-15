package universe.constellation.orion.viewer.test.espresso

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import java.util.concurrent.atomic.AtomicReference


abstract class CheckTapZoneStartScreen(private val showTapHelp: Boolean, intent: Intent) : InstrumentationTestCase() {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<OrionViewerActivity>(intent.apply {
        if (showTapHelp) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, true)
        } else {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
    })
}
@RunWith(AndroidJUnit4::class)
class NoBookNoStartTapScreen: CheckTapZoneStartScreen(true, BookDescription.SICP.toOpenIntent().apply { data = null;}) {
    @Test
    fun testStartScreenAbsent() {
        val options =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
        onView(withId(R.id.tap_help_close)).check(doesNotExist())
        assertTrue(options.isShowTapHelp)
    }
}

@RunWith(AndroidJUnit4::class)
class BookWithStartTapScreen: CheckTapZoneStartScreen(true, BookDescription.SICP.toOpenIntent()) {
    @Test
    fun testStartScreen() {
        val options =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
        assertTrue(options.isShowTapHelp)
        onView(withId(R.id.tap_help_close)).check(matches(isDisplayed()))
        checkSizeAndPosition()

        onView(withId(R.id.tap_help_close)).perform(click())
        //onView(withId(R.id.tap_help_close)).check(doesNotExist())//TODO
        assertFalse(options.isShowTapHelp)
    }

    private fun checkSizeAndPosition() {
        val rect = AtomicReference<Rect>()
        val loc = AtomicReference<IntArray>()
        activityScenarioRule.scenario.onActivity { it ->
            val r = Rect()
            (it.view as View).getLocalVisibleRect(r)
            rect.set(r)
            val l = IntArray(2)
            (it.view as View).getLocationOnScreen(l)
            loc.set(l)
        }

        onView(withId(R.id.tap_help_close)).check { view, noViewFoundException ->
            val dialogRect = Rect()
            val rootView = view.rootView
            rootView.getLocalVisibleRect(dialogRect)
            assertEquals(rect.get(), dialogRect)

            val l = IntArray(2)
            rootView.getLocationOnScreen(l)
            assertArrayEquals(loc.get(), l)
        }
    }
}