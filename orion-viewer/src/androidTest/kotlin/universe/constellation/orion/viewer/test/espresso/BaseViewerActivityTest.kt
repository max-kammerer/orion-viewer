package universe.constellation.orion.viewer.test.espresso

import android.content.Intent
import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.test.framework.BaseUITest
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.onActivity

/*Default zoom is "Fit Width"*/
abstract class BaseViewerActivityTest(
    val bookDescription: BookFile,
    startIntent: Intent = bookDescription.toOpenIntent(),
) : BaseUITest(startIntent) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book")
        fun testData(): Iterable<BookFile> {
            return BookFile.testEntriesWithCustoms()
        }
    }

    private lateinit var controller: Controller

    @Before
    fun checkStartInvariant() {
        awaitBookLoading()
        activityScenarioRule.scenario.onActivity {
            controller = it.controller!!
            Assert.assertEquals(it.controller!!.document.filePath, bookDescription.asPath())
            Assert.assertFalse(controller.pageLayoutManager.sceneRect.isEmpty)
        }
    }


    @After
    fun checkEndInvariant() {
        onActivity {
            if (::controller.isInitialized) {
                Assert.assertEquals(it.controller!!, controller)
            }
        }
    }


    protected fun applyZoom() {
        onView(ViewMatchers.withId(R.id.zoom_preview)).perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.zoom_picker_close)).perform(ViewActions.click())
    }

    protected fun applyGoTo() {
        onView(ViewMatchers.withId(R.id.page_preview)).perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.page_picker_close)).perform(ViewActions.click())
    }

    protected fun openZoom() {
        openMenuAndSelect(R.string.menu_zoom_text)
    }

    protected fun openGoTo() {
        openMenuAndSelect(R.string.menu_goto_text)
    }

    private fun openMenuAndSelect(textRes: Int) {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        val text = appContext.getString(textRes)
        if (isAtLeastKitkat()) {
            device.wait(Until.findObject(By.textContains(text)), 1000)
        }
        onView(ViewMatchers.withText(text)).perform(ViewActions.click())
    }
}

internal fun setSeekBarProgress(transform: (Int) -> Int): ViewAction {
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