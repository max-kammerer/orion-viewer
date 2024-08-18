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
import android.view.View
import universe.constellation.orion.viewer.OperationHolder
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.prefs.Preference

open class AndroidDevice @JvmOverloads constructor(
        private val wakeLockType: Int = PowerManager.SCREEN_BRIGHT_WAKE_LOCK
) : Device {

    private var screenLock: PowerManager.WakeLock? = null

    protected lateinit var activity: OrionBaseActivity

    protected lateinit var orionContext: OrionApplication

    private var delay: Preference<Int>? = null

    lateinit var options: GlobalOptions

    override val defaultTheme: String
        get() = GlobalOptions.APPLICATION_THEME

    override fun onKeyUp(keyCode: Int, isLongPress: Boolean, operation: OperationHolder): Boolean {
        //check mapped keys
        when (keyCode) {
            KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                operation.value = Device.PREV
                return true
            }
            KeyEvent.KEYCODE_SOFT_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                operation.value = Device.NEXT
                return true
            }
        }
        return false
    }

    open fun onCreate(activity: OrionBaseActivity) {
        options = activity.orionApplication.options

        if (activity.viewerType == Device.VIEWER_ACTIVITY) {
            delay = activity.orionApplication.options.SCREEN_BACKLIGHT_TIMEOUT
        }

        this.orionContext = activity.orionApplication
        val power = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        screenLock = power.newWakeLock(wakeLockType, "OrionViewer" + hashCode())
        screenLock!!.setReferenceCounted(false)
    }

    override fun onPause() {
        screenLock?.release()
    }

    override fun onWindowGainFocus() {
        delay()
    }

    override fun onUserInteraction() {
        delay()
    }

    private fun delay() {
        val value = delay?.value ?: return
        screenLock?.acquire(value * 1000 * 60.toLong())
    }

    override fun flushBitmap(view: View) {
        view.invalidate()
    }

    open fun fullScreen(on: Boolean, activity: Activity) {

    }
}
