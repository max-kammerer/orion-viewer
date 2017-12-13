/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017  Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.pdf

import android.graphics.Bitmap
import android.graphics.RectF
import com.artifex.mupdfdemo.MuPDFCore
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem

class PdfDocument @Throws(Exception::class) constructor(fileName: String) : Document {

    private val core: MuPDFCore = MuPDFCore(fileName)

    override val pageCount: Int
        get() = core.pageCount

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo = core.getPageInfo(pageNum)

    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) =
            core.renderPage(pageNumber, bitmap, zoom, left, top, right - left, bottom - top)

    override fun destroy() = core.onDestroy()

    override val title: String?
        get() = core.info.title

    override fun setContrast(contrast: Int) = core.setContrast(contrast)

    override fun setThreshold(threshold: Int) = core.setThreshold(threshold)

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        return core.textLines(pageNumber, RectF(absoluteX.toFloat(), absoluteY.toFloat(), (absoluteX + width).toFloat(), (absoluteY + height).toFloat()), singleWord)
    }

    override val outline: Array<OutlineItem>?
        get() {
            return core.outline?.takeIf { it.isNotEmpty() }?.let { items ->
                Array(items.size) {
                    index ->
                    OutlineItem(items[index].level, items[index].title, items[index].page)
                }
            }

        }

    override fun needPassword() = core.needsPassword()

    override fun authenticate(password: String) = core.authenticatePassword(password)

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? = core.searchPage(pageNumber, text)
}
