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

import android.graphics.Bitmap
import android.graphics.RectF
import universe.constellation.orion.viewer.outline.OutlineItem


interface PageInfoProvider {
    fun getPageInfo(pageNum: Int, cropMode: Int = CropMode.NO_MODE.cropMode/*TODO?*/): PageInfo
}

interface DocumentWrapper : PageInfoProvider {

    fun openDocument(fileName: String): Boolean

    val pageCount: Int

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo

    fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int)

    fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String?

    fun destroy()

    val title: String?

    fun setContrast(contrast: Int)

    fun setThreshold(threshold: Int)

    val outline: Array<OutlineItem>?

    fun needPassword(): Boolean

    fun authentificate(password: String): Boolean

    fun searchPage(pageNumber: Int, text: String): Array<RectF>?

    fun hasCalculatedPageInfo(pageNumber: Int): Boolean
}
