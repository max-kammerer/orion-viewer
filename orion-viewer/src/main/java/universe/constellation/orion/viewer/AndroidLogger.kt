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

package universe.constellation.orion.viewer

import android.util.Log

import java.io.FileWriter
import java.io.PrintWriter

object AndroidLogger : Logger {

    private const val LOGTAG = "OrionViewer"

    private var writer: PrintWriter? = null

    @JvmStatic
    fun startLogger(file: String) {
        if (writer != null) {
            stopLogger()
        }
        try {
            writer = PrintWriter(FileWriter(file))
        } catch (e: Exception) {
            e.printStackTrace()
            if (writer != null) {
                writer!!.close()
            }
        }
    }

    @JvmStatic
    fun stopLogger() {
        if (writer != null) {
            writer!!.close()
            writer = null
        }
    }

    override fun log(m: String?, e: Throwable) {
        m?.run { log(m) }
        log(e)
    }

    override fun log(m: String) {
        Log.d(LOGTAG, m)
        if (writer != null) {
            writer!!.write(m)
            writer!!.write("\n")
        }
    }

    private fun log(e: Exception) {
        Log.d(LOGTAG, e.message, e)
        if (writer != null) {
            e.printStackTrace(writer)
            writer!!.write("\n")
        }
    }

}
