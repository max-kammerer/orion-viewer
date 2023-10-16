package universe.constellation.orion.viewer.test.espresso

import android.graphics.Rect
import android.view.View
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.hamcrest.Matchers.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class NoBookNoStartTapScreen: InstrumentationTestCase(BookDescription.SICP.toOpenIntent().apply { data = null;}, true) {
    @Test
    fun testStartScreenAbsent() {
        onView(withId(R.id.tap_help_close)).check(doesNotExist())
        assertTrue(globalOptions.isShowTapHelp)
    }
}

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class BookWithStartTapScreen: InstrumentationTestCase(BookDescription.SICP.toOpenIntent(), true) {
    @Test
    fun testStartScreen() {
        assertTrue(globalOptions.isShowTapHelp)
        Thread.sleep(1000)
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

            displayRect.set(Rect(0, 0, it.display!!.width, it.display!!.height))
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