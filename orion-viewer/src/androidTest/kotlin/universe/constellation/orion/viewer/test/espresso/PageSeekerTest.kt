package universe.constellation.orion.viewer.test.espresso

import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.document.lastPageNum0
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.checkEquals
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.test.framework.toOpenIntentWithNewUI

@RunWith(Parameterized::class)
class PageSeekerTest(bookDescription: BookFile): BaseViewerActivityTest(bookDescription, bookDescription.toOpenIntentWithNewUI()) {

    @Test
    fun testSlowSwipe() {
        testGotoSwipe(Swipe.SLOW)
    }

    @Test
    fun testFastSwipe() {
        testGotoSwipe(Swipe.FAST)
    }

    private fun testGotoSwipe(swipe: Swipe) {
        openGoTo()
        val lastPageNumber = onActivity {
            it.controller!!.document.lastPageNum0
        }

        onView(withId(R.id.page_picker_seeker)).perform(GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.PINPOINT, InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.BUTTON_PRIMARY))
        onView(withId(R.id.page_picker_seeker)).perform(GeneralSwipeAction(
            swipe, GeneralLocation.CENTER_LEFT,
            GeneralLocation.CENTER_RIGHT, Press.PINPOINT
        ))

        checkEquals("Wrong page", lastPageNumber, onActivity {
            it.controller!!.currentPage
        })

        onView(withId(R.id.page_picker_seeker)).perform(GeneralSwipeAction(
            swipe, GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT, Press.FINGER
        ))
        onActivity {
            assertEquals(0, it.controller!!.currentPage)
        }
    }
}
