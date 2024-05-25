package universe.constellation.orion.viewer.test.espresso

import android.os.Build
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
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.checkEquals
import universe.constellation.orion.viewer.test.framework.checkTrue
import universe.constellation.orion.viewer.test.framework.toOpenIntentWithNewUI

@RunWith(Parameterized::class)
open class PageSeekerTest(bookDescription: BookFile): BaseViewerActivityTest(bookDescription, bookDescription.toOpenIntentWithNewUI()) {

    @Test
    fun testSlowSwipe() {
        testGotoSwipe(Swipe.SLOW)
    }

    @Test
    fun testFastSwipe() {
        testGotoSwipe(Swipe.FAST)
    }

    open fun configure() {

    }

    private fun testGotoSwipe(swipe: Swipe) {
        configure()
        openGoTo()
        val lastPageNumber = lastPageNumber0

        onView(withId(R.id.page_picker_seeker)).perform(GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.PINPOINT, InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.BUTTON_PRIMARY))
        onView(withId(R.id.page_picker_seeker)).perform(GeneralSwipeAction(
            swipe, GeneralLocation.CENTER_LEFT,
            GeneralLocation.CENTER_RIGHT, Press.PINPOINT
        ))

        checkPages(lastPageNumber, true)

        checkEquals("Wrong page", lastPageNumber, currentPage0)

        onView(withId(R.id.page_picker_seeker)).perform(GeneralSwipeAction(
            swipe, GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT, Press.FINGER
        ))
        checkPages(lastPageNumber, false)

        assertEquals(0, currentPage0)

    }

    private fun checkPages(lastPageNumber: Int, last: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val currentPage0 = currentPage0
            checkTrue(
                "Wrong page ${if (last) lastPageNumber else 0} != $currentPage0",
                if (last) 0 < currentPage0 else lastPageNumber > currentPage0
            )
            onView(withId(R.id.page_picker_seeker)).perform(
                GeneralClickAction(
                    Tap.SINGLE,
                    if (last) GeneralLocation.CENTER_RIGHT else GeneralLocation.CENTER_LEFT,
                    Press.PINPOINT,
                    InputDevice.SOURCE_MOUSE,
                    MotionEvent.BUTTON_PRIMARY
                )
            )
        }
    }
}
