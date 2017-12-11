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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

object Permissions {
    @JvmField
    val ORION_ASK_PERMISSION_CODE = 111

    @JvmStatic
    fun checkReadPermission(activity: Activity) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
            else true

    @JvmStatic
    fun checkWritePermission(activity: Activity) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else true

    @JvmStatic
    private fun checkPermission(activity: Activity, permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = activity.checkSelfPermission(permission)
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                log("Request permission " + permission)
                activity.requestPermissions(arrayOf(permission), ORION_ASK_PERMISSION_CODE)
                return false
            }
        }
        return true
    }
}
