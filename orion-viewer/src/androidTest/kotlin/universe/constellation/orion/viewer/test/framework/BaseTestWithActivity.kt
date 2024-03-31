package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule


abstract class BaseTestWithActivity(startIntent: Intent) : BaseTest() {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<OrionViewerActivity>(startIntent.apply {
        if (!hasExtra(GlobalOptions.SHOW_TAP_HELP)) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
        putExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, true)
    })

    @JvmField
    @Rule
    val screenshotRule = ScreenshotTakingRule()

    protected val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    val globalOptions by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
    }

    protected fun processEmulatorErrors() {
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

    protected fun awaitBookLoading() {
        lateinit var job: Job
        onActivity {
            job = it.openJob
        }
        runBlocking {
            job.join()
        }
    }
}

fun BaseTestWithActivity.doFail(message: String, namePrefix: String = name.methodName): Nothing {
    screenshotRule.takeScreenshot(namePrefix)
    logError(message)
    error(Assert.fail(message))
}

fun BaseTestWithActivity.checkNotEquals(message: String, expected: Int, actual: Int, namePrefix: String = name.methodName) {
    if (expected != actual) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertNotEquals(message, expected, actual)
    }
}

fun BaseTestWithActivity.checkTrue(message: String, condition: Boolean, namePrefix: String = name.methodName) {
    if (!condition) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertTrue(message, condition)
    }
}

fun <T: Any> BaseTestWithActivity.onActivity(body: (OrionViewerActivity) -> T): T {
    lateinit var res: T
    activityScenarioRule.scenario.onActivity {
        res = body(it)
    }
    return res
}

val instrumentationContext: Context
    get() = InstrumentationRegistry.getInstrumentation().context

val appContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext
