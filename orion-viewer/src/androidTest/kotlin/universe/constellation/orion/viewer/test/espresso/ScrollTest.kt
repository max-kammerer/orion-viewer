package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.document.lastPageNum0
import universe.constellation.orion.viewer.lastPageNum0
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.onActivity

@RunWith(Parameterized::class)
class ScrollTest(newUI: Boolean, bookDescription: BookFile): BaseViewerActivityTest(bookDescription, bookDescription.toOpenIntent {
    this.putExtra(GlobalOptions.NEW_UI, newUI)
}) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {1} book with newUI={0}")
        fun testData2(): Collection<Array<Any>> {
            return BookFile.testEntriesWithCustoms()
                .map { arrayOf(true, it) } + BookFile.testEntriesWithCustoms()
                .map { arrayOf(false, it) }

        }
    }

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
        onView(withId(R.id.page_picker_seeker)).perform(GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER))
        applyGoTo()
        onActivity {
            assertEquals(0, it.controller!!.currentPage)
        }

        onView(withId(R.id.view)).perform(swipeDown())
        activityScenarioRule.scenario.onActivity {
            assertEquals(it.controller!!.document.filePath, bookDescription.asPath())
            val first = it.controller!!.pageLayoutManager.activePages.first()
            assertEquals(0, first.pageNum)
            assertEquals(0f, first.layoutData.position.y, 0.0f)
        }
    }

    @Test
    fun testLastPageSwipeUp() {
        openGoTo()
        val lastPageNumber = onActivity {
            it.controller!!.lastPageNum0
        }

        onView(withId(R.id.page_picker_seeker)).perform(GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER))
        applyGoTo()
        onActivity {
            assertEquals(lastPageNumber, it.controller!!.currentPage)
        }

        onView(withId(R.id.view)).perform(swipeUp())

        onActivity { activity ->
            val pageLayoutManager = activity.controller!!.pageLayoutManager
            val last = pageLayoutManager.activePages.last()
            assertEquals(pageLayoutManager.activePages.joinToString(" ") {it.pageNum.toString()}, activity.controller!!.pageCount - 1, last.pageNum)
            assertTrue(last.layoutData.globalBottom <= pageLayoutManager.sceneRect.bottom)
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
