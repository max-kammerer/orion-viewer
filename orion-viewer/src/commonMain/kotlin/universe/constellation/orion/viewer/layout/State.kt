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

package universe.constellation.orion.viewer.layout

expect class State {
    var screenWidth: Int
    var screenHeight: Int

    var pageNumber: Int
    var rotation: Int

    //application default
    var screenOrientation: String

    var newOffsetX: Int
    var newOffsetY: Int

    var zoom: Int

    var leftMargin: Int
    var rightMargin: Int
    var topMargin: Int
    var bottomMargin: Int

    var enableEvenCropping: Boolean
    var cropMode: Int
    var leftEvenMargin: Int
    var rightEventMargin: Int

    var pageLayout: Int

    var contrast: Int
    var threshold: Int

    var walkOrder: String

    var colorMode: String

}