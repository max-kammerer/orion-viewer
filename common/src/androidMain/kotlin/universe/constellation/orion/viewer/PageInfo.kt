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

import universe.constellation.orion.viewer.layout.AutoCropMargins

actual data class PageInfo @JvmOverloads constructor(
        @JvmField actual val pageNum0: Int,
        /*used from jni*/
        @JvmField actual var width: Int = 0,
        /*used from jni*/
        @JvmField actual var height: Int = 0
) {

    @JvmField
    actual var autoCrop: AutoCropMargins? = null
}


actual data class PageSize @JvmOverloads constructor(
    /*used from jni*/
    @JvmField actual var width: Int = 0,
    /*used from jni*/
    @JvmField actual var height: Int = 0
) {

}
