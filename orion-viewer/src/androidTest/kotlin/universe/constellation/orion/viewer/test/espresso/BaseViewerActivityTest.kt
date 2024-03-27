package universe.constellation.orion.viewer.test.espresso

import android.content.Intent
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
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.BaseUITest
import universe.constellation.orion.viewer.view.OrionDrawScene

/*Default zoom is "Fit Width"*/
@SdkSuppress(minSdkVersion = 21)
open class BaseViewerActivityTest(
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

        if (Build.VERSION.SDK_INT >= 21) {
            device.wait(Until.findObject(By.clazz(OrionDrawScene::class.java)), 1000)
        }
    }


    @After
    fun checkEndInvariant() {
        activityScenarioRule.scenario.onActivity {
            if (::controller.isInitialized) {
                Assert.assertEquals(it.controller!!, controller)
            }
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
        if (Build.VERSION.SDK_INT >= 21) {
            device.wait(Until.findObject(By.textContains("Go To")), 1000)
        }
        Espresso.onView(ViewMatchers.withText("Go To")).perform(ViewActions.click())
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