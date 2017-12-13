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

package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.geometry.RectF

class OutlineItem(
        @JvmField val level: Int,
        @JvmField val title: String,
        @JvmField val page: Int
)

interface PageInfoProvider {
    fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo
}

/**
 * Perform contrast transformation and pixel filtering in native part
 * (it's more efficient)
 * */
interface ImagePostProcessor {

    /* filter watermark-pixels, see orion_bitmap.c */
    fun setThreshold(threshold: Int)

    /* contrast transformation will be applied to rendered image, see orion_bitmap.c */
    fun setContrast(contrast: Int)
}

interface Document : PageInfoProvider, ImagePostProcessor {

    val pageCount: Int

    val title: String?

    val outline: Array<OutlineItem>?

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo

    fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int)

    fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String?

    fun destroy()

    fun needPassword(): Boolean

    fun authenticate(password: String): Boolean

    fun searchPage(pageNumber: Int, text: String): Array<RectF>?

    fun hasCalculatedPageInfo(pageNumber: Int): Boolean = false
}
