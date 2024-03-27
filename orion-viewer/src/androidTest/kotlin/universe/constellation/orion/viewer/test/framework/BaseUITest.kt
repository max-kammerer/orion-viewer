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
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule


abstract class BaseUITest(startIntent: Intent, private val doGrantAction: Boolean = true) : BaseTest() {

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

    @Before
    fun grantPermissionsAndCheckInvariants() {
        processEmulatorErrors()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        if (doGrantAction && !BookDescription.SICP.asFile().canRead()) {
            val grant =
                device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT) ?: run {
                    //in case of problem with system UI
                    device.wait(Until.findObject(By.textContains("Wait")), SHORT_TIMEOUT)?.click()
                    device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT)
                        ?: error("Can't find grant action in warning dialog")
                }

            grant.click()

            val allowField = device.wait(Until.findObject(By.textContains("Allow")), LONG_TIMEOUT)
            allowField.click()
            device.wait(Until.findObject(By.checkable(true)), LONG_TIMEOUT)
            Assert.assertTrue(device.findObject(By.checkable(true)).isChecked)
            device.pressBack()
            Espresso.onView(ViewMatchers.withId(R.id.view))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.view))
                .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
            Assert.assertTrue(BookDescription.SICP.asFile().canRead())
        }
    }

    protected fun processEmulatorErrors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return

        repeat(3) {
            device.findObject(By.textContains("Wait"))?.click()
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

fun BaseUITest.doFail(message: String, namePrefix: String = name.methodName): Nothing {
    screenshotRule.takeScreenshot(namePrefix)
    logError(message)
    error(Assert.fail(message))
}

fun BaseUITest.checkTrue(message: String, condition: Boolean, namePrefix: String = name.methodName) {
    if (!condition) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertTrue(message, condition)
    }
}

fun <T: Any> BaseUITest.onActivity(body: (OrionViewerActivity) -> T): T {
    lateinit var res: T
    activityScenarioRule.scenario.onActivity {
        res = body(it)
    }
    return res
}

val instrumentationContext: Context
    get() = InstrumentationRegistry.getInstrumentation().context
