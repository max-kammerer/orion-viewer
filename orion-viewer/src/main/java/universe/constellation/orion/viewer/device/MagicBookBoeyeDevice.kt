package universe.constellation.orion.viewer.device

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.view.View

import universe.constellation.orion.viewer.OperationHolder
import universe.constellation.orion.viewer.prefs.OrionApplication

class MagicBookBoeyeDevice : EInkDevice() {

    override fun onKeyUp(keyCode: Int, isLongPress: Boolean, operation: OperationHolder): Boolean {
        if (isT62) {
            when (keyCode) {
                PAGE_UP, VOLUME_UP -> {
                    operation.value = Device.PREV
                    return true
                }
                PAGE_DOWN, VOLUME_DOWN -> {
                    operation.value = Device.NEXT
                    return true
                }
            }
        }

        return super.onKeyUp(keyCode, isLongPress, operation)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun fullScreen(on: Boolean, activity: Activity) {
        activity.window.decorView.systemUiVisibility = if (on) View.GONE else View.VISIBLE
    }

    companion object {

        private val MENU = 59
        private val F5 = 63
        private val HOME = 102
        private val PAGE_UP = 104
        private val PAGE_DOWN = 109
        private val VOLUME_DOWN = 114
        private val VOLUME_UP = 115
        private val POWER = 115
        private val NOTIFICATION = 143
        private val BACK = 158
        private val CAMERA = 212
        private val SEARCH = 217

        private val isT62 = "T62D".equals(OrionApplication.DEVICE, ignoreCase = true)
    }
}
