package universe.constellation.orion.viewer.filemanager

import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat


val supportedMimeTypes = arrayOf(
    "image/vnd.djvu",
    "image/x-djvu",
    "image/djvu",
    "application/djvu",
    "application/vnd.djvu",
    "application/pdf",
    "application/vnd.ms-xpsdocument",
    "application/oxps",
    "application/xps",
    "application/vnd.comicbook+zip",
    "application/x-cbz",
    "image/tiff",
    "image/x-tiff",
)

class ActionBarDrawerToggle(
    private val activity: OrionFileManagerActivityBase,
    drawerLayout: DrawerLayout,
    toolbar: androidx.appcompat.widget.Toolbar
) : ActionBarDrawerToggle(
    activity,
    drawerLayout,
    toolbar,
    R.string.fileopen_open_in_temporary_file,
    R.string.fileopen_open_in_temporary_file,
) {

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(val itemId = item.itemId) {
            R.id.nav_system_select ->  {
                if (!isAtLeastKitkat()) return true
                activity.selectDocumentInSystem?.launch(supportedMimeTypes)
                return true
            }
            R.id.nav_permissions ->  {
                activity.requestPermissions()
                return true
            }
            R.id.help_menu_item, R.id.about_menu_item -> {
                activity.openHelpActivity(itemId)
                return true
            }
            R.id.nav_settings -> {
                Action.OPTIONS.doAction(activity)
                return true
            }
            R.id.nav_exit -> {
                Action.CLOSE_ACTION.doAction(activity)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}