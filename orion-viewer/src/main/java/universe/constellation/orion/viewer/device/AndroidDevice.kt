/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.device

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.KeyEvent
import universe.constellation.orion.viewer.OperationHolder
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions

open class AndroidDevice @JvmOverloads constructor(
        private val wakeLockType: Int = PowerManager.SCREEN_BRIGHT_WAKE_LOCK
) : Device {

    private var screenLock: PowerManager.WakeLock? = null

    protected lateinit var activity: OrionBaseActivity

    private var delay = Device.DELAY

    lateinit var options: GlobalOptions

    lateinit var keyBinding: GlobalOptions

    override val isDefaultDarkTheme: Boolean
        get() = true

    override fun onKeyUp(keyCode: Int, isLongPress: Boolean, operation: OperationHolder): Boolean {
        //check mapped keys
        when (keyCode) {
            KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_VOLUME_UP -> {
                operation.value = Device.PREV
                return true
            }
            KeyEvent.KEYCODE_SOFT_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                operation.value = Device.NEXT
                return true
            }
        }
        return false
    }

    open fun onCreate(activity: OrionBaseActivity) {
        options = activity.orionContext.options
        keyBinding = activity.orionContext.keyBinding

        if (activity.viewerType == Device.VIEWER_ACTIVITY) {
            delay = activity.orionContext.options.getScreenBacklightTimeout(Device.VIEWER_DELAY) * 1000 * 60
        }
        this.activity = activity
        val power = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        screenLock = power.newWakeLock(wakeLockType, "OrionViewer" + hashCode())
        screenLock!!.setReferenceCounted(false)
    }

    override fun onPause() {
        screenLock?.release()
    }

    override fun onWindowGainFocus() {
        screenLock?.acquire(delay.toLong())
    }


    override fun onUserInteraction() {
        screenLock?.acquire(delay.toLong())
    }

    override fun flushBitmap() {
        activity.view?.invalidate()
    }

    open fun fullScreen(on: Boolean, activity: Activity) {

    }
}
