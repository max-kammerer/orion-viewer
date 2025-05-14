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

class LastPageInfo {
    @JvmField
    var screenWidth = 0
    @JvmField
    var screenHeight = 0
    @JvmField
    var pageNumber = 0
    @JvmField
    var rotation = 0

    //application default
    @JvmField
    var screenOrientation = "DEFAULT"
    @JvmField
    var newOffsetX = 0
    @JvmField
    var newOffsetY = 0
    @JvmField
    var zoom = 0
    @JvmField
    var leftMargin = 0
    @JvmField
    var rightMargin = 0
    @JvmField
    var topMargin = 0
    @JvmField
    var bottomMargin = 0
    @JvmField
    var enableEvenCropping = false
    @JvmField
    var cropMode = 0
    @JvmField
    var leftEvenMargin = 0
    @JvmField
    var rightEventMargin = 0
    @JvmField
    var pageLayout = 0
    @JvmField
    var contrast = 100
    @JvmField
    var threshold = 255

    @Transient
    @JvmField
    var fileData: String? = null

    @Transient
    @JvmField
    var fileSize: Long = 0

    @Transient
    @JvmField
    var simpleFileName: String? = null

    @Transient
    @JvmField
    var openingFileName: String? = null

    @Transient
    @JvmField
    var totalPages = 0

    @JvmField
    var walkOrder = "ABCD"
    @JvmField
    var colorMode = "CM_NORMAL"

    @JvmField
    var isSinglePageMode = true

    @JvmField
    var dictionary: String? = "DEFAULT"
}

