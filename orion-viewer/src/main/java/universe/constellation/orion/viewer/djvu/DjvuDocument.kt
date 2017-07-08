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

/**
 * User: mike
 * Date: 22.11.11
 * Time: 10:42
 */
class DjvuDocument(fileName: String) : Document {

    private var lastPage = -1
    private var doc: Long = 0
    private var context: Long = 0
    private val docInfo = DocInfo()

    override val outline: Array<OutlineItem>?
        get() = getOutline(doc)

    init {
        context = initContext()
        docInfo.fileName = fileName
        if (context == 0L) throw RuntimeException("Can't create djvu context").also { destroy() }
        doc = openFile(fileName, docInfo, context)
        if (doc == 0L) throw RuntimeException("Can't open file " + fileName).also { destroy() }
    }

    override val pageCount: Int
        get() = docInfo.pageCount

    override fun getPageInfo(pageNum: Int, cropMode: Int) =
            PageInfo(pageNum).also {
                timing("Page $pageNum info calculation") {
                    getPageInfo(doc, pageNum, it)
                }
            }

    @Synchronized
    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        gotoPage(pageNumber)
        timing("Page $pageNumber rendering") {
            drawPage(doc, bitmap, zoom.toFloat(), right - left, bottom - top, left, top, right - left, bottom - top)
        }
    }

    @Synchronized
    override fun destroy() {
        destroying(doc, context)
        doc = 0
        context = 0
    }

    @Synchronized
    private fun gotoPage(page: Int) {
        if (lastPage != page) {
            timing("Changing page...") {
                gotoPageInternal(doc,
                        if (page > docInfo.pageCount - 1)
                            docInfo.pageCount - 1
                        else if (page < 0)
                            0
                        else page
                )
                lastPage = page
            }
        }
    }

    override val title: String?
        get() = null

    override external fun setContrast(contrast: Int)

    override external fun setThreshold(threshold: Int)

    external fun getOutline(doc: Long): Array<OutlineItem>

    external fun getText(doc: Long, pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int): String

    override fun needPassword(): Boolean {
        return false
    }

    override fun authenticate(password: String): Boolean {
        return true
    }

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? {
        var text = text
        text = text.toLowerCase()

        val strings = ArrayList<String>(500)
        val positions = ArrayList<RectF>(500)
        getPageText(doc, pageNumber, strings, positions)

        var prevIndex = 0
        val indexes = ArrayList<Int>(500)
        val builder = StringBuilder()
        for (i in positions.indices) {
            val string = strings.get(i)
            builder.append(string.toLowerCase())
            val length = builder.length
            for (j in prevIndex..length - 1) {
                indexes.add(i)
            }
            prevIndex = length
        }

        val searchFrom = 0
        val result = ArrayList<RectF>()
        var i = builder.indexOf(text, searchFrom)
        while (i != -1) {
            val start = indexes[i]
            val end = indexes[i + text.length - 1]

            val rectF = RectF(getSafeRectInPosition(positions, start))
            rectF.union(getSafeRectInPosition(positions, end))
            result.add(rectF)
            i = i + text.length
            i = builder.indexOf(text, i)
        }

        return result.toTypedArray()
    }

    private fun getSafeRectInPosition(rects: List<RectF>, position: Int): RectF {
        //TODO
        return rects[position]
    }

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        return getText(doc, pageNumber, absoluteX, absoluteY, width, height)
    }

    override fun hasCalculatedPageInfo(pageNumber: Int): Boolean {
        return false
    }

    companion object {

        init {
            System.loadLibrary("djvu")
        }

        @JvmStatic
        external fun initContext(): Long

        @JvmStatic
        external fun openFile(filename: String, info: DocInfo, context: Long): Long


        @Synchronized
        @JvmStatic
        external fun gotoPageInternal(doc: Long, localActionPageNum: Int)

        @JvmStatic
        external fun getPageInfo(doc: Long, pageNum: Int, info: PageInfo): Int

        @JvmStatic
        @Synchronized
        external fun drawPage(doc: Long, bitmap: Bitmap, zoom: Float, pageW: Int, pageH: Int,
                              patchX: Int, patchY: Int,
                              patchW: Int, patchH: Int): Boolean

        @JvmStatic
        @Synchronized
        external fun destroying(doc: Long, context: Long)

        @JvmStatic
        @Synchronized
        external fun getPageText(doc: Long, pageNumber: Int, stringBuilder: ArrayList<*>, positions: ArrayList<*>): Boolean
    }
}
