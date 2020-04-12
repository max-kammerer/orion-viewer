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
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.artifex.mupdf.viewer.MuPDFCore
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem

class PdfDocument @Throws(Exception::class) constructor(fileName: String) : Document {

    private val core = MuPDFCore(fileName)

    override val pageCount: Int
        get() = core.countPages()

    override fun getPageInfo(pageNum: Int): PageInfo = core.getPageSize(pageNum).run {
        PageInfo(pageNum, x.toInt(), y.toInt())
    }

    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        core.drawPage(bitmap, pageNumber, left, top,  right, bottom, zoom.toFloat())
        //updateContrast(bitmap, bitmap.width * bitmap.height * 4)
    }

    override fun destroy() = core.onDestroy()

    override val title: String?
        get() = core.title

    external override fun setContrast(contrast: Int)
    external fun updateContrast(bitmap: Bitmap, size: Int)

    external override fun setThreshold(threshold: Int)

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        val text: StructuredText = core.getText(pageNumber)
        //TODO support single word
        return text.copy(Point(absoluteX.toFloat(), absoluteY.toFloat()), Point( (absoluteX + width).toFloat(), (absoluteY + height).toFloat()))
    }

    override val outline: Array<OutlineItem>?
        get() {
            fun collectItems(list: MutableList<OutlineItem>, items: Array<Outline>, level: Int) {
                items.forEach {
                    list.add(OutlineItem(level, it.title, it.page))
                    if (it.down != null) {
                        collectItems(list, it.down, level + 1)
                    }
                }
            }
            return core.outline?.takeIf { it.isNotEmpty() }?.let { items ->
                ArrayList<OutlineItem>(items.size).apply { collectItems(this, items, 0) }.toTypedArray()
            }

        }

    override fun needPassword() = core.needsPassword()

    override fun authenticate(password: String) = core.authenticatePassword(password)

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? = core.searchPage(pageNumber, text)?.map { it.toRect().run { RectF(x0, y0, x1, y1) } }?.toTypedArray()
}
