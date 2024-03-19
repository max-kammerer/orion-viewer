package universe.constellation.orion.viewer.test.espresso

import android.os.Build
import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.WAIT_TIMEOUT
import universe.constellation.orion.viewer.view.OrionDrawScene

@SdkSuppress(minSdkVersion = 21)
/*Default zoom is "Fit Width"*/
open class BaseEspressoTest(val bookDescription: BookFile) : InstrumentationTestCase(bookDescription.toOpenIntent()) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book")
        fun testData(): Iterable<BookFile> {
            return BookFile.testEntriesWithCustoms()
        }
    }

    @JvmField
    @Rule
    val screenshotRule = ScreenshotTakingRule()

    private lateinit var controller: Controller

    @Before
    fun checkStartInvariant() {
        lateinit var job: Job
        activityScenarioRule.scenario.onActivity {
            job = it.openJob
        }
        runBlocking {
            job.join()
        }
        activityScenarioRule.scenario.onActivity {
            controller = it.controller!!
            Assert.assertEquals(it.controller!!.document.filePath, bookDescription.asPath())
            Assert.assertFalse(controller.pageLayoutManager.sceneRect.isEmpty)
        }

        if (Build.VERSION.SDK_INT >= 21) {
            device.wait(Until.findObject(By.clazz(OrionDrawScene::class.java)), 1000)
        }
    }

    @After
    fun checkEndInvariant() {
        activityScenarioRule.scenario.onActivity {
            Assert.assertEquals(it.controller!!, controller)
        }
    }


    protected fun applyZoom() {
        Espresso.onView(ViewMatchers.withId(R.id.zoom_preview)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.zoom_picker_close)).perform(ViewActions.click())
    }

    protected fun applyGoTo() {
        Espresso.onView(ViewMatchers.withId(R.id.page_preview)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.page_picker_close)).perform(ViewActions.click())
    }

    protected fun openZoom() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        if (Build.VERSION.SDK_INT >= 21) {
            device.wait(Until.findObject(By.textContains("Zoom")), 1000)
        }
        Espresso.onView(ViewMatchers.withText("Zoom")).perform(ViewActions.click())
    }

    protected fun openGoTo() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        Espresso.onView(ViewMatchers.withText("Go To")).perform(ViewActions.click())
    }

    fun <T: Any> onActivity(body: (OrionViewerActivity) -> T): T {
        lateinit var res: T
        activityScenarioRule.scenario.onActivity {
            res = body(it)
        }
        return res
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