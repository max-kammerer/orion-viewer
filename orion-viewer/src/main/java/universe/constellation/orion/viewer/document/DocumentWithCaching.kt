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

package universe.constellation.orion.viewer.document

import android.graphics.Bitmap
import android.graphics.Rect
import android.support.v4.util.LruCache
import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.SimpleLayoutStrategy

/**
 * User: mike
 * Date: 15.10.11
 * Time: 9:53
 */

const private val WIDTH = 600
const private val HEIGH = 800

class DocumentWithCaching(val doc: DocumentWrapper) : DocumentWrapper by doc {

    private val cache = LruCache<Int, PageInfo?>(100)
    private val bitmap: Bitmap by lazy {
        Bitmap.createBitmap(WIDTH, HEIGH, Bitmap.Config.ARGB_8888);
    }

    private val layout = SimpleLayoutStrategy(this)

    private val bitmapArray: IntArray by lazy {
        IntArray(WIDTH * HEIGH)
    }

    override fun getPageInfo(pageNum: Int, autoCrop: Boolean): PageInfo? {
        synchronized(doc) {
            var pageInfo = cache.get(pageNum)
            if (pageInfo == null) {
                pageInfo = doc.getPageInfo(pageNum, autoCrop)
                cache.put(pageNum, pageInfo)
            }

            if (autoCrop && pageInfo?.autoCrop == null) {
                timing("Auto crop takes ") {
                    fillAutoCropInfo(pageInfo!!)
                }
            }

            return pageInfo
        }
    }

    private fun fillAutoCropInfo(page: PageInfo) {
        if (page.width == 0 || page.height == 0) {
            page.autoCrop = Rect(0, 0, page.width, page.height)
            return
        }

        val zoom = Math.floor(Math.sqrt(1.0 * WIDTH * HEIGH / (page.width * page.height)) * 10000) / 10000;

        //TOD size calc
        val newWidth = (zoom * page.width).toInt();
        val newHeight = (zoom * page.height).toInt();

//        val curPos = LayoutPosition()
//        layout.reset(curPos, true)
//        val leftTopCorner = layout.convertToPoint(curPos)
//
//
//        doc.renderPage(curPos.pageNumber, bitmap, curPos.docZoom, leftTopCorner.x, leftTopCorner.y, leftTopCorner.x + width, leftTopCorner.y + height)
        println("Calc auto crop for: ${page.width} x ${page.height}")
        println("Calc auto crop: $newWidth x $newHeight $zoom")
        timing("Auto crop page rendering: ") {
            doc.renderPage(page.pageNum0, bitmap, zoom, 0, 0, newWidth, newHeight)
        }

        timing("Auto crop data copy: ") {
            bitmap.getPixels(bitmapArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }

        println("${page.pageNum0}: ${page.width}x${page.height}")
        println("${page.pageNum0}: $zoom $newWidth $newHeight")

        val margins = timing("Auto crop margins calculation: ") {
            findMargins(ArrayImage(newWidth, newHeight, bitmapArray))
        }

        val marginsWithPadding = pad(margins, newWidth, newHeight)

        page.autoCrop = Rect(
                (marginsWithPadding.left / zoom).toInt(),
                (marginsWithPadding.top / zoom).toInt(),
                (marginsWithPadding.right / zoom).toInt(),
                (marginsWithPadding.bottom / zoom).toInt()

        )
        println("Zoomed result: ${page.pageNum0}: $margins $zoom")
        println("Unzoomed result: ${page.pageNum0}: ${page.autoCrop}")
    }
}

inline fun <R> timing(m: String,l: () -> R): R {
    var s = System.currentTimeMillis()
    val result = l()
    println("$m = ${System.currentTimeMillis() - s} ms")
    return result;
}

inline fun THRESHOLD(): Int = 245 //255 - 10
inline fun VOTE_THESHOLD(): Int = 3

abstract class Image(val width: Int, val height: Int) {

    abstract operator fun get(h: Int, w: Int): Int

    abstract operator fun set(h: Int, w: Int, color: Int)
}

class ArrayImage(width: Int, height: Int, @JvmField val source: IntArray): Image(width, height) {

    override operator fun get(h: Int, w: Int): Int {
        return source[w + h * width]
    }

    override operator fun set(h: Int, w: Int, color: Int) {
        source[w + h * width] = color
    }
}

//TODO replace 5 with dp  
fun pad(margins: Rect, newWidth: Int, newHeight: Int): Rect {
    val newMargins = Rect(margins)
    val widthPadding = max(newMargins.width() / 100, 5);
    val heightPadding = max(newMargins.height() / 100, 5);
    with(newMargins) {
        left = max(0, left - widthPadding)
        right = min(newWidth, right + widthPadding)

        top = max(0, top - heightPadding)
        bottom = min(newHeight, bottom + heightPadding)
    }

    return newMargins
}

fun findMargins(image: ArrayImage): Rect {
    timing("calc gradient") {
        calcGradient(image)
    }

    return timing("find rectangle") {
        findRectangle(image)
    }
}


fun calcGradient(image: ArrayImage) {
    val height = image.height
    val width = image.width
    val source = image.source
    val stroke = width

    var shift = 0
    for (h in 0..height - 2) {
        for (w in 0..width - 2) {
            val cur = w + shift
            val curInGray = gray(source[cur])
            val gradient = 255 - (abs(curInGray - gray(source[cur + 1])) + abs(curInGray - gray(source[cur + stroke]))) / 2
            source[cur] = rgb(gradient, gradient, gradient)
        }
        shift += stroke;
    }

    shift = (height - 1) * stroke
    for (w in 0..width - 2) {
        val cur = w + shift
        source[cur] = source[cur - stroke]
    }

    val w = width - 1
    for (h in 0..height - 1) {
        val cur = w + h * stroke
        source[cur] = source[cur - 1]
    }
}

fun findRectangle(grImage: ArrayImage): Rect {
    val height = grImage.height
    val width = grImage.width
    val source = grImage.source
    val stroke = width

    var left = width + 1
    var right = -1
    var top = height + 1
    var bottom = -1

    //calc votes
    for (h in 1..height - 2) {
        for (w in 1..width - 2) {
            val curIndex = w + h * stroke
            var votes = 0
            for (i in -1..1) {
                for (j in -1..1) {
                    val index = w + i + (h + j) * stroke
                    val value = red(source[index]);
                    if (value <= THRESHOLD()) {
                        votes++
                    }
                }
            }

            if (votes != 0) {
                source[curIndex] = agray(255 - votes, red(source[curIndex]))
            }
        }
    }

    //    val h = height - 1
    //    for (w in 0..width - 2) {
    //        val cur = w + h * stroke
    //        source[cur] = source[cur - stroke]
    //    }
    //
    //    val w = width - 1
    //    for (h in 0..height - 1) {
    //        val cur = w + h * stroke
    //        source[cur] = source[cur - 1]
    //    }
    //
    //
    //    for (h in 0..height - 1) {
    //        for (w in 0..width - 1) {
    //            val curIndex = w + h * stroke
    //            var votes = 0
    //            if (w == width - 1 || h == height - 1 || w == 0 || h == 0) {
    //
    //
    //            }
    //            if (votes != 0) {
    //                source[curIndex] = agray(255 - votes, red(source[curIndex]))
    //            }
    //        }
    //    }

    for (h in 0..height - 1) {
        for (w in 0..width - 1) {
            val curIndex = w + h * stroke
            if (alpha(source[curIndex]) <= 255 - VOTE_THESHOLD()) {
                left = min(left, w)
                right = max(right, w)

                top = min(top, h)
                bottom = max(bottom, h)
            }
        }
    }

    println("data1: ${Rect(left, top, right , bottom)}")

    if (right < 0) {
        right = width - 1
    }
    if (bottom < 0) {
        bottom = height - 1
    }

    if (left > width) {
        left = 0
    }
    if (top > height) {
        top = 0
    }

    val rectangle = Rect(left, top, right, bottom)
    println("data 2: $rectangle")
    return rectangle
}

inline fun gray(color: Int): Int {
    return (306 * red(color) + 601 * green(color) + 117 * blue(color))/1000;
}

inline fun alpha(color: Int): Int {
    return color.ushr(24)
}

inline fun red(color: Int): Int {
    return (color shr 16) and 255
}

inline fun green(color: Int): Int {
    return (color shr 8) and 255
}

inline fun blue(color: Int): Int {
    return color and 255
}

inline fun rgb(red: Int, green: Int, blue: Int): Int {
    return (255 shl 24) or (red shl 16) or (green shl 8) or blue
}


inline fun agray(alpha: Int, gray: Int): Int {
    return (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray
}

inline fun max(i1: Int, i2: Int): Int {
    return if (i1 > i2) i1 else i2
}

inline fun min(i1: Int, i2: Int): Int {
    return if (i1 < i2) i1 else i2
}

inline fun abs(i: Int): Int {
    return if ((i >= 0)) i else -i
}