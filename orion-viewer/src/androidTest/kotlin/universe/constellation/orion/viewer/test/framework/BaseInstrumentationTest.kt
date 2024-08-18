package universe.constellation.orion.viewer.test.framework

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule


abstract class BaseInstrumentationTest : BaseTest() {

    init {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        Espresso.setFailureHandler(EspressoFailureHandler(instrumentation))
    }


    @JvmField
    @Rule
    val screenshotRule = ScreenshotTakingRule()

    protected val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Before
    fun processEmulatorErrors() {
        if (!isAtLeastKitkat()) return

        device.waitForIdle()

        repeat(3) {
            if (device.findObject(By.textContains("System UI isn't responding")) != null) {
                device.findObject(By.textContains("Close"))?.click()
            }
            if (device.findObject(By.textContains("stopping")) != null) {
                //workaround for: bluetooth keeps stopping
                device.findObject(By.textContains("Close app"))?.click()
            }
            if (device.findObject(By.textContains("has stopped")) != null) {
                //workaround for: ... process has stopped
                device.findObject(By.textContains("OK"))?.click()
            }
        }
    }

    fun getStringRes(resId: Int): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(resId)
    }

    protected fun ActivityScenario<OrionViewerActivity>.checkFileWasOpened(fileName: String, pageCount: Int) {
        Espresso.onView(ViewMatchers.withId(R.id.view))
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        onActivity {
            Assert.assertNotNull(it.controller)
            Assert.assertEquals(
                fileName,
                it.controller!!.document.filePath.substringAfterLast("/")
            )
            Assert.assertEquals(
                pageCount,
                it.controller!!.document.pageCount
            )
        }
    }

    protected fun applyZoom() {
        Espresso.onView(ViewMatchers.withId(R.id.option_dialog_bottom_apply)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.option_dialog_bottom_close)).perform(ViewActions.click())
    }

    protected fun ActivityScenario<OrionViewerActivity>.applyGoTo() {
        val isNewUI = onActivityRes {
            it.globalOptions.isNewUI
        }
        if (!isNewUI) {
            Espresso.onView(ViewMatchers.withId(R.id.option_dialog_bottom_apply)).perform(ViewActions.click())
        } else {
            Espresso.onView(ViewMatchers.withId(R.id.view)).perform(ViewActions.click())
        }
    }

    fun ActivityScenario<OrionViewerActivity>.openZoom() {
        openMenuAndSelect(R.id.zoom_menu_item, R.string.menu_zoom_text)
    }

    fun ActivityScenario<OrionViewerActivity>.openGoTo() {
        openMenuAndSelect(-1, R.string.menu_goto_text)
    }

    fun ActivityScenario<OrionViewerActivity>.openCropDialog() {
        openMenuAndSelect(R.id.crop_menu_item, R.string.menu_crop_text)
    }

    fun ActivityScenario<OrionViewerActivity>.openBookOptions() {
        openMenuAndSelect(R.id.book_options_menu_item, R.string.menu_book_preferences)
    }

    fun applyCrop() {
        Espresso.onView(ViewMatchers.withId(R.id.option_dialog_bottom_apply)).perform(ViewActions.click())
        closeDialog()
    }

    fun closeDialog() {
        Espresso.onView(ViewMatchers.withId(R.id.option_dialog_bottom_close)).perform(ViewActions.click())
    }

    fun ActivityScenario<OrionViewerActivity>.openMenuAndSelect(id: Int, resId: Int) {
        val newUI = onActivityRes {
            it.showMenu()
            it.isNewUI
        }
        if (newUI) {
            if (id != -1) {
                Espresso.onView(ViewMatchers.withId(id)).perform(ViewActions.click())
            }
        } else {
            val text = appContext.getString(resId)
            if (isAtLeastKitkat()) {
                device.wait(Until.findObject(By.textContains(text)), 1000)
            }
            Espresso.onView(ViewMatchers.withText(resId)).perform(ViewActions.click())
        }
    }
}