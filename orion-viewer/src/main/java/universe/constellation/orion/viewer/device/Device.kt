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
import android.view.KeyEvent
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.OperationHolder
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.document.Document

interface Device {

    fun onKeyUp(keyCode: Int, event: KeyEvent, operation: OperationHolder): Boolean

    fun onCreate(activity: OrionBaseActivity)

    fun onNewBook(info: LastPageInfo, document: Document)

    fun onBookClose(currentPage: Int, pageCount: Int)

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
