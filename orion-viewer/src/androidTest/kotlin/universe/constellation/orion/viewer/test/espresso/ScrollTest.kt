package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookDescription

@RunWith(Parameterized::class)
class ScrollTest(bookDescription: BookDescription): BaseEspressoTest(bookDescription) {

    @Test
    fun testSwipeUpAndDown3() {
       testSwipeUpAndDown(3)
    }

    @Test
    fun testSwipeUpAndDown10() {
        testSwipeUpAndDown(10)
    }


    private fun testSwipeUpAndDown(downScale: Int) {
        openZoom()
        onView(withId(R.id.zoom_picker_seeker)).perform(setSeekBarProgress { it / downScale })
        applyZoom()
        Thread.sleep(500)
        onView(withId(R.id.view)).perform(swipeUp())
        Thread.sleep(500)
        onView(withId(R.id.view)).perform(swipeUp())
        Thread.sleep(500)
        onView(withId(R.id.view)).perform(swipeUp())
        Thread.sleep(500)
        onView(withId(R.id.view)).perform(swipeDown())
    }
}