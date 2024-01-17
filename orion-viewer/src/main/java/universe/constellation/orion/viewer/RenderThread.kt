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
import android.graphics.Rect
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import java.util.concurrent.ConcurrentLinkedQueue

private const val BITMAP_CACHE_SIZE = 10

open class BitmapCache : Thread() {

    private val cachedBitmaps = ConcurrentLinkedQueue<CacheInfo>()

    protected class CacheInfo(/*val info: LayoutPosition,*/ val bitmap: Bitmap) {
        var isValid = true
    }

    fun createBitmap(width: Int, height: Int): Bitmap {
        var bitmap: Bitmap? = null
        if (cachedBitmaps.size >= BITMAP_CACHE_SIZE) {
            //TODO: add checks
            val nonValids = cachedBitmaps.asSequence().filter { !it.isValid }
            val cacheInfo =
                nonValids.firstOrNull { info -> width <= info.bitmap.width && height <= info.bitmap.height }

            if (cacheInfo == null) {
                val info = nonValids.firstOrNull() ?: error("cache is full")
                cachedBitmaps.remove(info)
                info.bitmap.recycle()
            } else {
                bitmap = cacheInfo.bitmap
                cacheInfo.isValid = true
            }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } else {
            log("Using cached bitmap $bitmap")
        }
        addToCache(CacheInfo(bitmap))
        log("create bitmap: $width $height + $bitmap")
        return bitmap
    }

    protected fun addToCache(info: CacheInfo) {
        cachedBitmaps.add(info)
    }

    fun free(info: Bitmap) {
        for (next in cachedBitmaps) {
            if (next.bitmap === info) {
                next.isValid = false
                break
            }
        }
    }

    fun invalidateCache() {
        for (next in cachedBitmaps) {
            next.isValid = false
        }
        log("Cache invalidated")
    }
}

fun renderInner(bound: Rect, curPos: LayoutPosition, page: Int, doc: Document, bitmap: Bitmap, layout: LayoutStrategy): Bitmap {
    println("Rendering $page: $bound $curPos")
    doc.renderPage(page, bitmap, curPos.docZoom, bound.left, bound.top,  bound.right, bound.bottom, curPos.x.marginLess, curPos.y.marginLess)
    return bitmap
}
