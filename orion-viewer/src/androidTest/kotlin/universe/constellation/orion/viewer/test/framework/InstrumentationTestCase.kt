package universe.constellation.orion.viewer.test.framework

import android.content.Intent
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule


abstract class InstrumentationTestCase(intent: Intent, private val showTapHelp: Boolean = false, additionalParams: (Intent) -> Unit = {}) : BaseTest() {

    protected val device by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @get:Rule
    var activityScenarioRule = activityScenarioRule<OrionViewerActivity>(intent.apply {
        if (showTapHelp) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, true)
        } else {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
        intent.putExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, true)
        additionalParams(this)
    })

    @JvmField
    @Rule
    val screenshotRule = ScreenshotTakingRule()

    val globalOptions by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
    }
}