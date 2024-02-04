package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookDescription

@RunWith(Parameterized::class)
class ScrollTest(bookDescription: BookDescription): BaseEspressoTest(bookDescription) {

    @Test
    fun testSwipeUpAndDownWithZoomIn3() {
        testSwipeUpAndDown(0.5f)
    }

    @Test
    fun testSwipeUpAndDownWithZoomOut3() {
       testSwipeUpAndDown(3)
    }

    @Test
    fun testSwipeUpAndDownWithZoomOut10() {
        testSwipeUpAndDown(10)
    }

    @Test
    fun testFirstPageSwipeDown() {
        openGoTo()
        onView(withId(R.id.page_picker_seeker)).perform(setSeekBarProgress { 0 })
        applyGoTo()
        onView(withId(R.id.view)).perform(swipeDown())
        activityScenarioRule.scenario.onActivity {
            Assert.assertEquals(it.controller!!.pageCount, bookDescription.pageCount)
            val first = it.controller!!.pageLayoutManager.visiblePages.first()
            Assert.assertEquals(0, first.pageNum)
            Assert.assertEquals(0f, first.layoutData.position.y, 0.0f)
        }
    }

    @Test
    fun testLastPageSwipeUp() {
        openGoTo()
        onView(withId(R.id.page_picker_seeker)).perform(setSeekBarProgress { bookDescription.pageCount - 1 })
        applyGoTo()
        onView(withId(R.id.view)).perform(swipeUp())

        activityScenarioRule.scenario.onActivity { activity ->
            val pageLayoutManager = activity.controller!!.pageLayoutManager
            val last = pageLayoutManager.visiblePages.last()
            Assert.assertEquals(pageLayoutManager.visiblePages.joinToString(" ") {it.pageNum.toString()}, activity.controller!!.pageCount - 1, last.pageNum)
            Assert.assertTrue(last.layoutData.globalBottom <= pageLayoutManager.sceneRect.bottom)
        }
    }

    private fun testSwipeUpAndDown(downScale: Int) {
        testSwipeUpAndDown(downScale.toFloat())
    }

    private fun testSwipeUpAndDown(downScale: Float) {
        openZoom()
        onView(withId(R.id.zoom_picker_seeker)).perform(setSeekBarProgress { (it / downScale).toInt() })
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
