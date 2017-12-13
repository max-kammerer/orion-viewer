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

import android.graphics.Point
import universe.constellation.orion.viewer.layout.State

interface LayoutStrategy {

    val zoom: Int

    val margins: CropMargins

    val rotation: Int

    val layout: Int

    val walker: PageWalker

    fun nextPage(pos: LayoutPosition): Int

    fun prevPage(pos: LayoutPosition): Int

    fun reset(pos: LayoutPosition, pageNumber: Int)

    fun reset(pos: LayoutPosition, pageNumber: Int, forward: Boolean)

    fun changeRotation(rotation: Int): Boolean

    fun changeOverlapping(horizontal: Int, vertical: Int): Boolean

    fun reset(info: LayoutPosition, forward: Boolean, pageInfo: PageInfo, cropMode: Int, zoom: Int, doCentering: Boolean)

    fun changeZoom(zoom: Int): Boolean

    fun changeCropMargins(cropMargins: CropMargins): Boolean

    fun init(info: State, options: PageOptions)

    fun serialize(info: LastPageInfo)

    fun convertToPoint(pos: LayoutPosition): Point

    fun changeWalkOrder(walkOrder: String): Boolean

    fun changePageLayout(pageLayout: Int): Boolean

    fun setDimension(width: Int, height: Int)

}

fun LayoutStrategy.calcPageLayout(layoutInfo: LayoutPosition, nextNotPrev: Boolean, pageCount: Int) {
    val result =
            if (nextNotPrev)
                nextPage(layoutInfo)
            else
                prevPage(layoutInfo)

    when (result) {
        0 -> return
        1 ->
            if (layoutInfo.pageNumber < pageCount - 1) {
                reset(layoutInfo, layoutInfo.pageNumber + 1)
            }
        -1 ->
            if (layoutInfo.pageNumber > 0) {
                reset(layoutInfo, layoutInfo.pageNumber - 1, false)
            }
        else -> throw RuntimeException("Unknown result $result")
    }
}