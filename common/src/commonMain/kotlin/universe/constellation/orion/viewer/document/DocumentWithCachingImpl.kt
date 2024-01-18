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

@file:Suppress("NOTHING_TO_INLINE")
package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.geometry.Rect
import universe.constellation.orion.viewer.layout.*
import kotlin.math.floor
import kotlin.math.sqrt

private const val WIDTH = 600
private const val HEIGHT = 800

private const val THRESHOLD = 255 - 10
private const val VOTE_THRESHOLD = 3



class DocumentWithCachingImpl(val doc: Document) : DocumentWithCaching, Document by doc {

    private val cache = createCache<Int, PageInfo?>(100)

    private val bitmap: Bitmap by lazy {
        createBitmap(WIDTH, HEIGHT)
    }

    //TODO: ugly hack
    lateinit var strategy: SimpleLayoutStrategy

    private val bitmapArray: IntArray by lazy {
        IntArray(WIDTH * HEIGHT)
    }

    fun resetCache() {
        cache.evictAll()
    }

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo {
        return doc.mySynchronized {
            var pageInfo = cache.get(pageNum)
            if (pageInfo == null) {
                pageInfo = doc.getPageInfo(pageNum)
                cache.put(pageNum, pageInfo)
            }

            if (cropMode != 0 && (pageInfo.autoCrop == null)) {
                timing("Full auto crop") {
                    fillAutoCropInfo(pageInfo, cropMode)
                }
            }

            pageInfo
        }
    }

    private fun fillAutoCropInfo(page: PageInfo, cropMode: Int) {
        if (page.width == 0 || page.height == 0) {
            page.autoCrop = AutoCropMargins(0, 0, 0, 0)
            return
        }


        val curPos = LayoutPosition()

        //Crop manual/no mode margins to calc new width and height
        val firstCropMode = if (cropMode.toMode.isManualFirst()) CropMode.MANUAL else CropMode.NO_MODE
        log("Calculating first reset with page = $page with cropMode = $firstCropMode")
        strategy.reset(curPos, true, page, firstCropMode.cropMode, 10000, false)

        val pageWidth1R = curPos.x.pageDimension
        val pageHeight1R = curPos.y.pageDimension
        log("Cur pos 1R = $curPos")
        log("New dimension: $pageWidth1R x $pageHeight1R")

        if (pageWidth1R == 0 || pageHeight1R == 0) {
            page.autoCrop = null
            return
        }

        //zoom page to fit crop screen
        val zoomInDouble = floor(sqrt(1.0 * WIDTH * HEIGHT / (pageWidth1R * pageHeight1R)) * 10000) / 10000
        strategy.reset(curPos, true, page, firstCropMode.cropMode, (zoomInDouble * 10000).toInt(), false)
        val newWidth = curPos.x.pageDimension
        val newHeight = curPos.y.pageDimension

        log("Cur pos for crop screen: $newWidth x $newHeight $zoomInDouble")

        timing("Render page for auto crop processing") {
            val leftTopCorner = strategy.convertToPoint(curPos)
            //TODO:
            doc.renderPage(curPos.pageNumber, bitmap, curPos.docZoom, leftTopCorner.x, leftTopCorner.y, leftTopCorner.x + newWidth, leftTopCorner.y + newHeight, 0, 0)
        }

        timing("Extract pixels from bitmap") {
            bitmap.getPixels(bitmapArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight())
        }

        val margins = timing("Calculate margins") {
            findMargins(ArrayImage(newWidth, newHeight, bitmapArray))
        }

        val marginsWithPadding = pad(margins, newWidth, newHeight)

        page.autoCrop = AutoCropMargins(
                (marginsWithPadding.left / zoomInDouble).toInt(),
                (marginsWithPadding.top / zoomInDouble).toInt(),
                (marginsWithPadding.right / zoomInDouble).toInt(),
                (marginsWithPadding.bottom / zoomInDouble).toInt()

        )
        log("Zoomed result: ${page.pageNum0}: $margins $zoomInDouble")
        log("Unzoomed result: ${page.pageNum0}: ${page.autoCrop}")
    }

    override fun hasCalculatedPageInfo(pageNumber: Int): Boolean = cache[pageNumber] != null

    override fun toString(): String {
        return doc.toString()
    }
}

abstract class Image(val width: Int, val height: Int) {

    abstract operator fun get(h: Int, w: Int): Int

    abstract operator fun set(h: Int, w: Int, color: Int)
}

class ArrayImage(width: Int, height: Int, val source: IntArray): Image(width, height) {

    override operator fun get(h: Int, w: Int): Int = source[w + h * width]

    override operator fun set(h: Int, w: Int, color: Int) {
        source[w + h * width] = color
    }
}

//TODO replace 5 with dp  
fun pad(margins: AutoCropMargins, newWidth: Int, newHeight: Int): AutoCropMargins {
    return with(margins) {
        val widthPadding = max((newWidth - left - right) / 100, 5)
        val heightPadding = max((newHeight - top - bottom) / 100, 5)

        val left = max(0, left - widthPadding)
        val right = max(0, right - widthPadding)

        val top = max(0, top - heightPadding)
        val bottom = max(0, bottom - heightPadding)
        AutoCropMargins(left, top, right, bottom)
    }
}

fun findMargins(image: ArrayImage): AutoCropMargins {
    timing("Calc gradient") {
        calcGradient(image)
    }

    return timing("Find rectangle") {
        findRectangle(image)
    }
}


fun calcGradient(image: ArrayImage) {
    val height = image.height
    val width = image.width
    val source = image.source
    val stroke = width

    var shift = 0
    for (h in 0 until height - 1) {
        for (w in 0 until width - 1) {
            val cur = w + shift
            val curInGray = gray(source[cur])
            val gradient = 255 - (abs(curInGray - gray(source[cur + 1])) + abs(curInGray - gray(source[cur + stroke]))) / 2
            source[cur] = rgb(gradient, gradient, gradient)
        }
        shift += stroke
    }

    shift = (height - 1) * stroke
    for (w in 0 until width - 1) {
        val cur = w + shift
        source[cur] = source[cur - stroke]
    }

    val w = width - 1
    for (h in 0 until height) {
        val cur = w + h * stroke
        source[cur] = source[cur - 1]
    }
}

fun findRectangle(grImage: ArrayImage): AutoCropMargins {
    val height = grImage.height
    val width = grImage.width
    val source = grImage.source
    val stroke = width

    var left = width + 1
    var right = -1
    var top = height + 1
    var bottom = -1

    //calc votes
    for (h in 1 until height - 1) {
        for (w in 1 until width - 1) {
            val curIndex = w + h * stroke
            var votes = 0
            for (i in -1..1) {
                for (j in -1..1) {
                    val index = w + i + (h + j) * stroke
                    val value = red(source[index])
                    if (value <= THRESHOLD) {
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

    for (h in 0 until height) {
        for (w in 0 until width) {
            val curIndex = w + h * stroke
            if (alpha(source[curIndex]) <= 255 - VOTE_THRESHOLD) {
                left = min(left, w)
                right = max(right, w)

                top = min(top, h)
                bottom = max(bottom, h)
            }
        }
    }

    log("data1: ${Rect(left, top, right , bottom)}")

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
    log("data 2: $rectangle")
    return AutoCropMargins(left, top, width - right, height - bottom)
}

inline fun gray(color: Int): Int = (306 * red(color) + 601 * green(color) + 117 * blue(color))/1000

inline fun alpha(color: Int): Int = color.ushr(24)

inline fun red(color: Int): Int = (color shr 16) and 255

inline fun green(color: Int): Int = (color shr 8) and 255

inline fun blue(color: Int): Int = color and 255

inline fun rgb(red: Int, green: Int, blue: Int): Int =
        (255 shl 24) or (red shl 16) or (green shl 8) or blue


inline fun agray(alpha: Int, gray: Int): Int =
        (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray

inline fun max(i1: Int, i2: Int): Int = if (i1 > i2) i1 else i2

inline fun min(i1: Int, i2: Int): Int = if (i1 < i2) i1 else i2

inline fun abs(i: Int): Int = if (i >= 0) i else -i