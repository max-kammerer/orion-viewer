package universe.constellation.orion.viewer.test

import android.content.Intent
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions

internal fun OrionViewerActivity.updateGlobalOptionsFromIntent(intent: Intent): Boolean {
    if (!intent.hasExtra(GlobalOptions.TEST_FLAG) ||
        !intent.getBooleanExtra(GlobalOptions.TEST_FLAG, false)) {
        return false
    }

    if (intent.hasExtra(GlobalOptions.SHOW_TAP_HELP)) {
        val showTapHelp = intent.getBooleanExtra(GlobalOptions.SHOW_TAP_HELP, false)
        globalOptions.saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, showTapHelp)
    }

    if (intent.hasExtra(GlobalOptions.OLD_UI)) {
        val oldUI = intent.getBooleanExtra(GlobalOptions.OLD_UI, false)
        globalOptions.saveBooleanProperty(GlobalOptions.OLD_UI, oldUI)
    }

    if (intent.hasExtra(GlobalOptions.APPLICATION_THEME)) {
        val theme = intent.getStringExtra(GlobalOptions.APPLICATION_THEME)!!
        globalOptions.saveStringProperty(GlobalOptions.APPLICATION_THEME, theme)
    }

    if (intent.hasExtra(GlobalOptions.LONG_TAP_ACTION)) {
        val action = intent.getStringExtra(GlobalOptions.LONG_TAP_ACTION)!!
        globalOptions.saveStringProperty(GlobalOptions.LONG_TAP_ACTION, action)
    }

    if (intent.hasExtra(GlobalOptions.TEST_SCREEN_WIDTH) && intent.hasExtra(GlobalOptions.TEST_SCREEN_HEIGHT)) {
        val newWidth =
            intent.getIntExtra(GlobalOptions.TEST_SCREEN_WIDTH, view.layoutParams.width)
        val newHeigth =
            intent.getIntExtra(GlobalOptions.TEST_SCREEN_HEIGHT, view.layoutParams.height)
        view.layoutParams.width = newWidth
        view.layoutParams.height = newHeigth
        view.requestLayout()
    }
    return intent.getBooleanExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, false)
}

internal fun OrionViewerActivity.resetSettingInTest(intent: Intent) {
    if (!intent.hasExtra(GlobalOptions.TEST_FLAG) ||
        !intent.getBooleanExtra(GlobalOptions.TEST_FLAG, false)) {
        return
    }

    globalOptions.prefs.edit().clear().apply()
}