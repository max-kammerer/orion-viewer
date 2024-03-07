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
import universe.constellation.orion.viewer.PageDimension
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy

expect class OutlineItem {
    val level: Int
    val title: String
    val page: Int
}

interface PageInfoProvider {
    fun getPageInfo(layoutStrategy: SimpleLayoutStrategy, cropMode: Int): PageInfo
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

interface Page {

    val pageNum: Int

    fun getPageDimension(): PageDimension

    fun renderPage(bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int, leftOffset: Int, topOffset: Int)

    fun readPageDataForRendering()

    fun destroy()
}

abstract class AbstractDocument(override val filePath: String) : Document {

    private val pages = HashMap<Int, PageWithAutoCrop>()

    @Synchronized
    final override fun getOrCreatePageAdapter(pageNum: Int): PageWithAutoCrop {
        return pages.getOrPut(pageNum) { createPage(pageNum) }
    }

    abstract fun createPage(pageNum: Int): PageWithAutoCrop

    @Synchronized
    fun destroyPages() {
        ArrayList(pages.values).forEach {
            destroyPage(it)
        }
        pages.clear()
    }

    @Synchronized
    override fun destroyPage(page: Page) {
        pages.remove(page.pageNum)
        page.destroy()
    }

    override fun toString(): String {
        return filePath
    }
}

interface Document : ImagePostProcessor {

    val pageCount: Int

    val filePath: String

    val title: String?

    val outline: Array<OutlineItem>?

    fun getOrCreatePageAdapter(pageNum: Int): PageWithAutoCrop

    fun getText(pageNum: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String?

    fun destroy()

    fun destroyPage(page: Page)

    fun needPassword(): Boolean = false

    fun authenticate(password: String): Boolean = true

    fun searchPage(pageNumber: Int, text: String): Array<RectF>?
}
