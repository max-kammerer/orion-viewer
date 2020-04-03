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
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem

class PdfDocument @Throws(Exception::class) constructor(fileName: String) : Document {

    private val core: com.artifex.mupdf.fitz.Document = com.artifex.mupdf.fitz.Document.openDocument(fileName)

    override val pageCount: Int
        get() = core.countPages()

    override fun getPageInfo(pageNum: Int): PageInfo = core.loadPage(pageNum).bounds.run {
        PageInfo(pageNum, (x1 - x0).toInt(), (y1 - y0).toInt())
    }

    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        val device = AndroidDrawDevice(bitmap, left, top, 0, 0, right - left, bottom - top)
        try {
            core.loadPage(pageNumber).run(device, Matrix(zoom.toFloat(), zoom.toFloat()))
        } finally {
            device.destroy()
        }
        updateContrast(bitmap, bitmap.width * bitmap.height * 4)
    }

    override fun destroy() = core.destroy()

    override val title: String?
        get() = core.getMetaData(com.artifex.mupdf.fitz.Document.META_INFO_TITLE)

    override external fun setContrast(contrast: Int)
    external fun updateContrast(bitmap: Bitmap, size: Int)

    external override fun setThreshold(threshold: Int)

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        val text: StructuredText = core.loadPage(pageNumber).toStructuredText()
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
            return core.loadOutline()?.takeIf { it.isNotEmpty() }?.let { items ->
                ArrayList<OutlineItem>(items.size).apply { collectItems(this, items, 0) }.toTypedArray()
            }

        }

    override fun needPassword() = core.needsPassword()

    override fun authenticate(password: String) = core.authenticatePassword(password)

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? = core.loadPage(pageNumber).search(text)?.map { it.toRect().run { RectF(x0, y0, x1, y1) } }?.toTypedArray()
}
