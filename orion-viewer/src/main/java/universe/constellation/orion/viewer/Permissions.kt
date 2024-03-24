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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

object Permissions {
    const val ASK_PERMISSION_COMMON = 111
    const val ASK_READ_PERMISSION_FOR_BOOK_OPEN = 112
    const val ASK_READ_PERMISSION_FOR_FILE_MANAGER = 113

    @JvmStatic
    fun checkReadPermission(activity: Activity, code: Int = ASK_PERMISSION_COMMON, doRequest: Boolean = true) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE, code, doRequest)
            else true

    @JvmStatic
    fun Activity.checkAndRequestStorageAccessPermissionOrReadOne(code: Int, doRequest: Boolean = true): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                if (doRequest) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, code)
                }
            } else {
                return true
            }
        } else {
            return checkReadPermission(this, code, doRequest)
        }
        return false
    }

    @JvmStatic
    fun checkWritePermission(activity: Activity, code: Int = ASK_PERMISSION_COMMON) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, code)
            else true

    @JvmStatic
    private fun checkPermission(activity: Activity, permission: String, code: Int = ASK_PERMISSION_COMMON, doRequest: Boolean = true): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = activity.checkSelfPermission(permission)
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                if (doRequest) {
                    log("Request ($code) permission $permission")
                    activity.requestPermissions(arrayOf(permission), code)
                }
                return false
            }
        }
        return true
    }
}
