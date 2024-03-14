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

package universe.constellation.orion.viewer

import android.graphics.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition


fun getPrefKey(keyCode: Int, isLong: Boolean): String {
    return keyCode.toString() + if (isLong) "long" else ""
}

fun LayoutPosition.toAbsoluteRect(): RectF {
    val left = x.offset + x.marginLeft
    val top = y.offset + y.marginLeft
    return RectF(left.toFloat(), top.toFloat(), (left + x.screenDimension).toFloat(), (top + y.screenDimension).toFloat())///
}

inline fun errorInDebug(message: String) {
    if (BuildConfig.DEBUG) {
        log("Error: $message")
        error(message)
    }
}

inline fun <T> errorInDebugOr(message: String, elseBody: () -> T) : T {
    errorInDebug(message)
    return elseBody()
}