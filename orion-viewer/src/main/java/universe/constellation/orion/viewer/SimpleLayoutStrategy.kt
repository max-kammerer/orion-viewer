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

import android.graphics.Point
import universe.constellation.orion.viewer.document.Document

import universe.constellation.orion.viewer.document.DocumentWithCaching
import universe.constellation.orion.viewer.document.PageInfoProvider
import universe.constellation.orion.viewer.prefs.GlobalOptions

class SimpleLayoutStrategy private constructor(
        private val pageInfoProvider: PageInfoProvider,
        private val pageCount: Int
) : LayoutStrategy {

    private var viewWidth = 0

    private var viewHeight = 0

    private var VERTICAL_OVERLAP = 3

    private var HORIZONTAL_OVERLAP = 3

    override var margins = CropMargins()
        private set

    override var zoom: Int = 0
        private set

    override var rotation: Int = 0
        private set

    override var walker = PageWalker("default", this)
        private set

    override var layout: Int = 0
        private set

    override fun nextPage(pos: LayoutPosition) {
        if (walker.next(pos)) {
            if (pos.pageNumber < pageCount - 1) {
                reset(pos, pos.pageNumber + 1)
            }
        }
        Common.d("new position: $pos")
    }

    override fun prevPage(pos: LayoutPosition) {
        if (walker.prev(pos)) {
            if (pos.pageNumber > 0) {
                reset(pos, pos.pageNumber - 1, false)
            }
        }

        Common.d("new position: $pos")
    }

    override fun changeRotation(rotation: Int): Boolean {
        if (this.rotation != rotation) {
            this.rotation = rotation
            return true
        }
        return false
    }

    override fun changeOverlapping(horizontal: Int, vertical: Int): Boolean {
        if (HORIZONTAL_OVERLAP != horizontal || VERTICAL_OVERLAP != vertical) {
            HORIZONTAL_OVERLAP = horizontal
            VERTICAL_OVERLAP = vertical
            return true
        }
        return false
    }

    override fun reset(pos: LayoutPosition, pageNumber: Int) {
        reset(pos, pageNumber, true)
    }

    override fun reset(pos: LayoutPosition, pageNumber: Int, forward: Boolean) {
        var pageNum = pageNumber
        if (pageCount - 1 < pageNum) {
            pageNum = pageCount - 1
        }
        if (pageNum < 0) {
            pageNum = 0
        }

        //original width and height without cropped margins
        reset(pos, forward, pageInfoProvider.getPageInfo(pageNum, margins.cropMode), margins.cropMode, zoom, true)
    }

    override fun reset(info: LayoutPosition, forward: Boolean, pageInfo: PageInfo, cropMode: Int, zoom: Int, doCentering: Boolean) {
        info.rotation = rotation
        info.pageNumber = pageInfo.pageNum0

        val pageWidth = pageInfo.width
        val pageHeight = pageInfo.height
        resetMargins(info, pageWidth, pageHeight)

        val isEvenPage = (pageInfo.pageNum0 + 1) % 2 == 0
        val mode = cropMode.toMode

        val autoCrop = pageInfo.autoCrop
        if (autoCrop != null && CropMode.AUTO_MANUAL === mode) {
            appendAutoCropMargins(info, autoCrop)
        }

        if (mode.hasManual()) {
            val leftMargin = if (margins.evenCrop && isEvenPage) margins.evenLeft else margins.left
            val rightMargin = if (margins.evenCrop && isEvenPage) margins.evenRight else margins.right
            appendManualMargins(info, leftMargin, rightMargin)
        }

        if (autoCrop != null && mode.hasAuto() && CropMode.AUTO_MANUAL !== mode) {
            appendAutoCropMargins(info, autoCrop)
        }

        info.x.screenDimension = if (rotation == 0) viewWidth else viewHeight
        info.y.screenDimension = if (rotation == 0) viewHeight else viewWidth

        info.screenWidth = viewWidth
        info.screenHeight = viewHeight

        //set zoom and zoom margins and dimensions
        info.setDocZoom(zoom)

        info.x.marginLess = (info.docZoom * info.x.marginLess).toInt()
        info.y.marginLess = (info.docZoom * info.y.marginLess).toInt()

        //zoomed with and height
        info.x.pageDimension = (info.docZoom * info.x.pageDimension).toInt()
        info.y.pageDimension = (info.docZoom * info.y.pageDimension).toInt()

        info.x.overlap = info.x.screenDimension * HORIZONTAL_OVERLAP / 100
        info.y.overlap = info.y.screenDimension * VERTICAL_OVERLAP / 100
        //System.out.println("overlap " + hOverlap + " " + vOverlap);

        walker.reset(info, forward, doCentering)
    }

    private fun appendManualMargins(info: LayoutPosition, leftMargin: Int, rightMargin: Int) {
        val pageWidth = info.x.pageDimension
        val pageHeight = info.y.pageDimension

        val xLess = (leftMargin.toDouble() * pageWidth.toDouble() * 0.01).toInt()
        val xMore = (pageWidth.toDouble() * rightMargin.toDouble() * 0.01).toInt()
        val yLess = (margins.top.toDouble() * pageHeight.toDouble() * 0.01).toInt()
        val yMore = (pageHeight.toDouble() * margins.bottom.toDouble() * 0.01).toInt()

        info.x.marginLess += xLess
        info.x.marginMore += xMore
        info.y.marginLess += yLess
        info.y.marginMore += yMore

        info.x.pageDimension -= xLess + xMore
        info.y.pageDimension -= yLess + yMore
    }

    private fun appendAutoCropMargins(info: LayoutPosition, autoCrop: AutoCropMargins) {
        info.x.marginLess += autoCrop.left
        info.x.marginMore += autoCrop.right
        info.y.marginLess += autoCrop.top
        info.y.marginMore += autoCrop.bottom

        info.x.pageDimension -= autoCrop.left + autoCrop.right
        info.y.pageDimension -= autoCrop.top + autoCrop.bottom
    }

    private fun resetMargins(info: LayoutPosition, pageWidth: Int, pageHeight: Int) {
        info.x.marginLess = 0
        info.y.marginLess = 0
        info.x.marginMore = 0
        info.y.marginMore = 0
        info.x.pageDimension = pageWidth
        info.y.pageDimension = pageHeight
    }

    override fun changeZoom(zoom: Int): Boolean {
        if (this.zoom != zoom) {
            this.zoom = zoom
            return true
        }
        return false
    }

    override fun changeNavigation(walkOrder: String): Boolean {
        if (walkOrder.walkOrder != walker.direction) {
            walker = PageWalker(walkOrder, this)
            return true
        }
        return false
    }

    override fun changePageLayout(navigation: Int): Boolean {
        if (this.layout != navigation) {
            this.layout = navigation
            return true
        }
        return false
    }

    override fun changeCropMargins(margins: CropMargins): Boolean {
        if (margins != this.margins) {
            this.margins = margins
            return true
        }

        return false
    }


    override fun init(info: LastPageInfo, options: GlobalOptions) {
        changeCropMargins(CropMargins(info.leftMargin, info.rightMargin, info.topMargin, info.bottomMargin, info.leftEvenMargin, info.rightEventMargin, info.enableEvenCropping, info.cropMode))
        changeRotation(info.rotation)
        changeZoom(info.zoom)
        changeNavigation(info.walkOrder)
        changePageLayout(info.pageLayout)
        changeOverlapping(options.horizontalOverlapping, options.verticalOverlapping)
    }

    override fun serialize(info: LastPageInfo) {
        info.screenHeight = viewHeight
        info.screenWidth = viewWidth

        info.leftMargin = margins.left
        info.rightMargin = margins.right
        info.topMargin = margins.top
        info.bottomMargin = margins.bottom
        info.leftEvenMargin = margins.evenLeft
        info.rightEventMargin = margins.evenRight
        info.enableEvenCropping = margins.evenCrop
        info.cropMode = margins.cropMode

        info.rotation = rotation
        info.zoom = zoom
        info.walkOrder = walker.direction.name
        info.pageLayout = layout
    }

    override fun convertToPoint(pos: LayoutPosition): Point {
        return Point(pos.x.marginLess + pos.x.offset, pos.y.marginLess + pos.y.offset)
    }

    override fun setDimension(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    companion object {

        @JvmStatic
        fun create(doc: Document): SimpleLayoutStrategy {
            val simpleLayoutStrategy = SimpleLayoutStrategy(doc, doc.pageCount)
            if (doc is DocumentWithCaching) {
                //TODO: ugly hack
                doc.strategy = simpleLayoutStrategy
            }
            return simpleLayoutStrategy
        }
    }
}