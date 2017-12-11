/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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
import android.os.Build
import android.view.KeyEvent
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.document.Document

interface Device {

    object Info {
        @JvmField
        val MANUFACTURER = getField("MANUFACTURER")
        @JvmField
        val MODEL = getField("MODEL")
        @JvmField
        val DEVICE = getField("DEVICE")
        @JvmField
        val HARDWARE = getField("HARDWARE")

        @JvmField
        val ONYX_DEVICE = "ONYX".equals(MANUFACTURER, ignoreCase = true) && OnyxUtil.isEinkDevice()

        @JvmField
        val TEXET_TB_138 = "texet".equals(DEVICE, ignoreCase = true) && "rk29sdk".equals(MODEL, ignoreCase = true)

        @JvmField
        val TEXET_TB176FL = "texet".equals(MANUFACTURER, ignoreCase = true) && "TB-176FL".equals(DEVICE, ignoreCase = true) && "TB-176FL".equals(MODEL, ignoreCase = true)

        @JvmField
        val TEXET_TB576HD = "texet".equals(MANUFACTURER, ignoreCase = true) && "TB-576HD".equals(DEVICE, ignoreCase = true) && "TB-576HD".equals(MODEL, ignoreCase = true)

        @JvmField
        val RK30SDK = "rk30sdk".equals(MODEL, ignoreCase = true) && ("T62D".equals(DEVICE, ignoreCase = true) || DEVICE.toLowerCase().contains("onyx"))

        fun getField(name: String): String =
                try {
                    Build::class.java.getField(name).get(null) as String
                } catch (e: Exception) {
                    log("Exception on extracting Build property:" + name)
                    ""
                }


        @JvmField
        val version: String = Build.VERSION.INCREMENTAL

    }

    fun onKeyUp(keyCode: Int, event: KeyEvent, operation: OperationHolder): Boolean

    fun onCreate(activity: OrionBaseActivity)

    fun onNewBook(info: LastPageInfo, document: Document)

    fun onBookClose(info: LastPageInfo)

    fun onDestroy()

    fun onPause()

    fun onWindowGainFocus()

    fun onUserInteraction()

    fun flushBitmap()

    val isDefaultDarkTheme: Boolean

    fun fullScreen(on: Boolean, activity: Activity)

    companion object {

        const val DELAY = 1 //1 min

        const val VIEWER_DELAY = 10 //10 min

        const val NEXT = 1

        const val PREV = -1

        const val ESC = 10

        const val DEFAULT_ACTIVITY = 0

        const val VIEWER_ACTIVITY = 1
    }
}
