package universe.constellation.orion.viewer.test.framework

import android.content.Intent
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication


abstract class InstrumentationTestCase(intent: Intent, private val showTapHelp: Boolean = false, additionalParams: (Intent) -> Unit = {}) : BaseTest() {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<OrionViewerActivity>(intent.apply {
        if (showTapHelp) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, true)
        } else {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
        additionalParams(this)
    })

    val globalOptions by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
    }
}