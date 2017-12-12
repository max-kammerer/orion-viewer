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

import android.graphics.RectF

/**
 * User: mike
 * Date: 15.10.11
 * Time: 18:49
 */
data class LayoutPosition(
        var x: OneDimension = OneDimension(),
        var y: OneDimension = OneDimension(),
        var pageNumber: Int = 0,
        var screenWidth: Int = 0,
        var screenHeight: Int = 0,
        var rotation: Int = 0,
        //need in equals?
        var docZoom: Double = 0.0
) {

    fun deepCopy() = copy(x = x.copy(), y = y.copy())

    fun setDocZoom(zoom: Int) {
        if (zoom <= 0) {
            when (zoom) {
            //fit width
                0 -> docZoom = x.screenDimension.toDouble() / x.pageDimension
            //fit height
                -1 -> docZoom = y.screenDimension.toDouble() / y.pageDimension
            //fit page
                -2 -> docZoom = Math.min(x.screenDimension.toDouble() / x.pageDimension, y.screenDimension.toDouble() / y.pageDimension)
            }
        } else {
            docZoom = (0.0001f * zoom).toDouble()
        }
    }

    fun toAbsoluteRect(): RectF {
        val left = x.offset + x.marginLess
        val top = y.offset + y.marginLess
        return RectF(left.toFloat(), top.toFloat(), (left + x.screenDimension).toFloat(), (top + y.screenDimension).toFloat())///
    }
}
