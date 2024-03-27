package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule


abstract class BaseUITest(intent: Intent, private val showTapHelp: Boolean = false, forceGrantAction: Boolean = true, additionalParams: (Intent) -> Unit = {}) : BaseTest(forceGrantAction) {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<OrionViewerActivity>(intent.apply {
        if (showTapHelp) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, true)
        } else {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
        putExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, true)
        additionalParams(this)
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

fun BaseUITest.failWithScreenShot(message: String, namePrefix: String = name.methodName): Nothing {
    screenshotRule.takeScreenshot(namePrefix)
    logError(message)
    error(Assert.fail(message))
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
