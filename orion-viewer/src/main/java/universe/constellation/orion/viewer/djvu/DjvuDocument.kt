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

package universe.constellation.orion.viewer.djvu

import android.graphics.Bitmap
import android.graphics.RectF
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.pdf.DocInfo
import universe.constellation.orion.viewer.timing
import java.util.*

class DjvuDocument(fileName: String) : Document {

    private var lastPage = -1
    private var lastPagePointer = 0L
    private var docPointer = 0L
    private var contextPointer = 0L
    private val docInfo = DocInfo()

    override val outline: Array<OutlineItem>?
        get() = getOutline(docPointer)

    init {
        contextPointer = initContext()
        docInfo.fileName = fileName
        if (contextPointer == 0L) throw RuntimeException("Can't create djvu contextPointer").also { destroy() }
        docPointer = openFile(fileName, docInfo, contextPointer)
        if (docPointer == 0L) throw RuntimeException("Can't open file $fileName").also { destroy() }
    }

    override val pageCount: Int
        get() = docInfo.pageCount

    override fun getPageInfo(pageNum: Int, cropMode: Int) =
            PageInfo(pageNum).also {
                timing("Page $pageNum info calculation") {
                    getPageInfo(docPointer, pageNum, it)
                }
            }

    @Synchronized
    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        val pagePointer = gotoPage(pageNumber)
        timing("Page $pageNumber rendering") {
            drawPage(docPointer, pagePointer, bitmap, zoom.toFloat(), right - left, bottom - top, left, top, right - left, bottom - top)
        }
    }

    @Synchronized
    override fun destroy() {
        releasePage()
        destroying(docPointer, contextPointer)
        docPointer = 0
        contextPointer = 0
    }

    private fun releasePage() {
        releasePage(lastPagePointer)
        lastPagePointer = 0
    }

    @Synchronized
    private fun gotoPage(page: Int): Long {
        if (lastPage != page) {
            timing("Changing page...") {
                releasePage()
                lastPagePointer = gotoPageInternal(docPointer,
                        when {
                            page > docInfo.pageCount - 1 -> docInfo.pageCount - 1
                            page < 0 -> 0
                            else -> page
                        }
                )
                lastPage = page
            }
        }
        return lastPagePointer
    }

    override val title: String?
        get() = null

    external override fun setContrast(contrast: Int)
    external override fun setThreshold(threshold: Int)
    external fun getOutline(doc: Long): Array<OutlineItem>
    external fun getText(doc: Long, pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int): String
    external fun releasePage(page: Long)

    override fun needPassword() = false

    override fun authenticate(password: String) = true

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? {
        val textToSearch = text.toLowerCase()

        val strings = ArrayList<String>(500)
        val positions = ArrayList<RectF>(500)
        getPageText(docPointer, pageNumber, strings, positions)

        var prevIndex = 0
        val indexes = ArrayList<Int>(500)
        val builder = StringBuilder()
        for (i in positions.indices) {
            val string = strings[i]
            builder.append(string.toLowerCase())
            val length = builder.length
            for (j in prevIndex until length) {
                indexes.add(i)
            }
            prevIndex = length
        }

        val searchFrom = 0
        val result = ArrayList<RectF>()
        val textLength = textToSearch.length
        var i = builder.indexOf(textToSearch, searchFrom)
        while (i != -1) {
            val start = indexes[i]
            val end = indexes[i + textLength - 1]

            val rectF = RectF(getSafeRectInPosition(positions, start))
            rectF.union(getSafeRectInPosition(positions, end))
            result.add(rectF)
            i += textLength
            i = builder.indexOf(textToSearch, i)
        }

        return result.toTypedArray()
    }

    private fun getSafeRectInPosition(rects: List<RectF>, position: Int): RectF {
        //TODO
        return rects[position]
    }

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean) =
            getText(docPointer, pageNumber, absoluteX, absoluteY, width, height)

    override fun hasCalculatedPageInfo(pageNumber: Int): Boolean = false

    companion object {

        init {
            System.loadLibrary("djvu")
        }

        @JvmStatic
        external fun initContext(): Long

        @JvmStatic
        external fun openFile(filename: String, info: DocInfo, context: Long): Long

        @JvmStatic
        external fun gotoPageInternal(doc: Long, pageNum: Int): Long

        @JvmStatic
        external fun getPageInfo(doc: Long, pageNum: Int, info: PageInfo): Int

        @JvmStatic
        external fun drawPage(doc: Long, page: Long, bitmap: Bitmap, zoom: Float, pageW: Int, pageH: Int,
                              patchX: Int, patchY: Int,
                              patchW: Int, patchH: Int): Boolean

        @JvmStatic
        external fun destroying(doc: Long, context: Long)

        @JvmStatic
        external fun getPageText(doc: Long, pageNumber: Int, stringBuilder: ArrayList<*>, positions: ArrayList<*>): Boolean
    }
}
