package universe.constellation.orion.viewer

import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.internal.view.SupportMenuItem
import universe.constellation.orion.viewer.prefs.GlobalOptions

enum class OptionActions(@JvmField val key: String) {
    NONE("NONE"),

    FULL_SCREEN("FULL_SCREEN") {
        override fun doAction(activity: OrionViewerActivity, newValue: Boolean) {
            activity.window.setFlags(
                if (newValue) WindowManager.LayoutParams.FLAG_FULLSCREEN else 0,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            activity.device!!.fullScreen(newValue, activity)
        }
    },

    SHOW_ACTION_BAR("SHOW_ACTION_BAR") {
        override fun doAction(activity: OrionViewerActivity, newValue: Boolean) {
            if (activity.isNewUI) return
            val toolbar = activity.toolbar
            val layoutParams = toolbar.layoutParams
            if (newValue) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                layoutParams.height = 0
            }
            toolbar.layoutParams = layoutParams
            activity.invalidateMenu()
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

    open fun doAction(activity: OrionViewerActivity,  newValue: Boolean) {

    }

}
