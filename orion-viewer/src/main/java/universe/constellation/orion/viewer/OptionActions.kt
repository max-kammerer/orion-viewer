package universe.constellation.orion.viewer

import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.internal.view.SupportMenuItem
import universe.constellation.orion.viewer.prefs.GlobalOptions

enum class OptionActions(@JvmField val key: String) {
    NONE("NONE"),

    FULL_SCREEN("FULL_SCREEN") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
            activity.window.setFlags(
                if (newValue) WindowManager.LayoutParams.FLAG_FULLSCREEN else 0,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            activity.device!!.fullScreen(newValue, activity)
        }
    },

    SHOW_ACTION_BAR("SHOW_ACTION_BAR") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
            if (activity.isNewUI) return
            val toolbar = activity.toolbar
            val layoutParams = toolbar.layoutParams
            if (newValue) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                layoutParams.height = 0
            }
            toolbar.layoutParams = layoutParams
            val menu = toolbar.menu
            menu.clear()
            activity.onCreateOptionsMenu(menu)
            if (!newValue) {
                for (i in 0 until menu.size()) {
                    val item = menu.getItem(i)
                    item.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_NEVER)
                }
            }
        }
    },

    SHOW_STATUS_BAR("SHOW_ACTION_BAR") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
            activity.statusBarHelper.setShowStatusBar(newValue)
        }
    },

    SHOW_OFFSET_ON_STATUS_BAR("SHOW_OFFSET_ON_STATUS_BAR") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
            activity.statusBarHelper.setShowOffset(newValue)
        }
    },

    SHOW_TIME_ON_STATUS_BAR("SHOW_TIME_ON_STATUS_BAR") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
            activity.statusBarHelper.setShowClock(newValue)
        }
    },

    SCREEN_OVERLAPPING_HORIZONTAL("SCREEN_OVERLAPPING_HORIZONTAL") {
        override fun doAction(activity: OrionViewerActivity, hor: Int, ver: Int) {
            val controller = activity.controller
            controller?.changeOverlap(hor, ver)
        }
    },

    SCREEN_OVERLAPPING_VERTICAL("SCREEN_OVERLAPPING_VERTICAL") {
        override fun doAction(activity: OrionViewerActivity, hor: Int, ver: Int) {
            val controller = activity.controller
            controller?.changeOverlap(hor, ver)
        }
    },

    SET_CONTRAST("contrast") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Int, newValue: Int) {
            val controller = activity.controller
            controller?.changeContrast(newValue)
        }
    },

    SET_THRESHOLD("threshold") {
        override fun doAction(activity: OrionViewerActivity, oldValue: Int, newValue: Int) {
            val controller = activity.controller
            controller?.changeThreshhold(newValue)
        }
    };

    open fun doAction(activity: OrionViewerActivity, oldValue: Int, newValue: Int) {
    }

    fun doAction(activity: OrionViewerActivity?, globalOptions: GlobalOptions?) {
    }

    open fun doAction(activity: OrionViewerActivity, oldValue: Boolean, newValue: Boolean) {
    }
}
