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

    override fun fullScreen(on: Boolean, activity: Activity) {
        activity.window.decorView.systemUiVisibility = if (on) View.GONE else View.VISIBLE
    }

    companion object {

        private const val MENU = 59
        private const val F5 = 63
        private const val HOME = 102
        private const val PAGE_UP = 104
        private const val PAGE_DOWN = 109
        private const val VOLUME_DOWN = 114
        private const val VOLUME_UP = 115
        private const val POWER = 115
        private const val NOTIFICATION = 143
        private const val BACK = 158
        private const val CAMERA = 212
        private const val SEARCH = 217

        private val isT62 = "T62D".equals(OrionApplication.DEVICE, ignoreCase = true)
    }
}
