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


interface Logger {
    fun log(m: String) {
        if (isDebugBuild) {
            println(m)
        }
    }

    fun log(m: String?, e: Throwable) {
        if (m != null) {
            println(m)
        }
        e.printStackTrace()
    }
}

var logger = object : Logger {}

/**
 * Page layout and rendering produce hundreds of log lines per second while the book is scrolled
 * or zoomed. Writing them costs more than the rendering itself, so the trace is muted until the
 * debug log is switched on in the preferences.
 */
var isTraceEnabled = false

inline fun logTrace(message: () -> String) {
    if (isTraceEnabled) {
        log(message())
    }
}

fun log(m: String) = logger.log(m)

fun logError(m: String) = logger.log("Error: $m")

fun log(e: Throwable) = logger.log(e.message, e)

fun log(m: String, e: Exception) {
    logger.log(m, e)
}

inline fun errorInDebug(message: String, ex: Throwable? = null) {
    logError(message)
    ex?.printStackTrace()
    if (isDebugBuild) {
        error(message)
    }
}

inline fun <T> errorInDebugOr(message: String, elseBody: () -> T) : T {
    errorInDebug(message)
    return elseBody()
}