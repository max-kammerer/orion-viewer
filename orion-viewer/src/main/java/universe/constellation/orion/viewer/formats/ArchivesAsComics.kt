package universe.constellation.orion.viewer.formats

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/* Declared disabled in the manifest: without the option archives don't offer the app (issue #234). */
private const val ALIAS = "universe.constellation.orion.viewer.ArchivesAsComicsAlias"

/**
 * Offers the app for zip and rar files of other apps, or stops to. The system keeps this state
 * apart from the options, so it is also checked on start: a restored backup brings only the option.
 */
fun Context.setArchivesAsComics(enabled: Boolean) {
    val alias = ComponentName(this, ALIAS)
    val newState =
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    val isEnabled = packageManager.getComponentEnabledSetting(alias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    if (isEnabled != enabled) {
        packageManager.setComponentEnabledSetting(alias, newState, PackageManager.DONT_KILL_APP)
    }
}
