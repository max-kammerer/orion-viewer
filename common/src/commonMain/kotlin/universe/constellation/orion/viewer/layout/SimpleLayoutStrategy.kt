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

package universe.constellation.orion.viewer.layout

import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.PageOptions
import universe.constellation.orion.viewer.PageWalker
import universe.constellation.orion.viewer.geometry.Point
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.walkOrder

class SimpleLayoutStrategy private constructor(
        private val center: Boolean = false
) : LayoutStrategy {

    override var viewWidth = 0
        private set

    override var viewHeight = 0
        private set

    private var VERTICAL_OVERLAP = 3

    private var HORIZONTAL_OVERLAP = 3

    override var margins = CropMargins()
        private set

    override var zoom: Int = 0
        private set

    override var rotation: Int = 0
        private set

    override var walker = PageWalker("default".walkOrder)
        private set

    override var layout: Int = 0
        private set

    override fun nextPage(pos: LayoutPosition): Int {
        if (walker.next(pos, layout)) {
            return 1
        }
        log("new position: $pos")
        return 0
    }

    override fun prevPage(pos: LayoutPosition): Int {
        if (walker.prev(pos, layout)) {
            return -1
        }

        log("new position: $pos")
        return 0
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

    override fun reset(pos: LayoutPosition, page: PageInfo, forward: Boolean) {
        reset(pos, forward, page, margins.cropMode, zoom, center)
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

        info.x.marginLeft = (info.docZoom * info.x.marginLeft).toInt()
        info.y.marginLeft = (info.docZoom * info.y.marginLeft).toInt()

        //zoomed with and height
        info.x.pageDimension = (info.docZoom * info.x.pageDimension).toInt()
        info.y.pageDimension = (info.docZoom * info.y.pageDimension).toInt()

        info.x.overlap = info.x.screenDimension * HORIZONTAL_OVERLAP / 100
        info.y.overlap = info.y.screenDimension * VERTICAL_OVERLAP / 100
        //System.out.println("overlap " + hOverlap + " " + vOverlap);

        walker.reset(info, forward, doCentering, layout)
    }

    private fun appendManualMargins(info: LayoutPosition, leftMargin: Int, rightMargin: Int) {
        val pageWidth = info.x.pageDimension
        val pageHeight = info.y.pageDimension

        val xLess = (leftMargin.toDouble() * pageWidth.toDouble() * 0.01).toInt()
        val xMore = (pageWidth.toDouble() * rightMargin.toDouble() * 0.01).toInt()
        val yLess = (margins.top.toDouble() * pageHeight.toDouble() * 0.01).toInt()
        val yMore = (pageHeight.toDouble() * margins.bottom.toDouble() * 0.01).toInt()

        info.x.marginLeft += xLess
        info.x.marginRight += xMore
        info.y.marginLeft += yLess
        info.y.marginRight += yMore

        info.x.pageDimension -= xLess + xMore
        info.y.pageDimension -= yLess + yMore
    }

    private fun appendAutoCropMargins(info: LayoutPosition, autoCrop: AutoCropMargins) {
        info.x.marginLeft += autoCrop.left
        info.x.marginRight += autoCrop.right
        info.y.marginLeft += autoCrop.top
        info.y.marginRight += autoCrop.bottom

        info.x.pageDimension -= autoCrop.left + autoCrop.right
        info.y.pageDimension -= autoCrop.top + autoCrop.bottom
    }

    private fun resetMargins(info: LayoutPosition, pageWidth: Int, pageHeight: Int) {
        info.x.marginLeft = 0
        info.y.marginLeft = 0
        info.x.marginRight = 0
        info.y.marginRight = 0
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

    override fun changeWalkOrder(walkOrder: String): Boolean {
        val newWalkOrder = walkOrder.walkOrder
        if (newWalkOrder != walker.order) {
            walker = PageWalker(newWalkOrder)
            return true
        }
        return false
    }

    override fun changePageLayout(pageLayout: Int): Boolean {
        if (this.layout != pageLayout) {
            this.layout = pageLayout
            return true
        }
        return false
    }

    override fun changeCropMargins(cropMargins: CropMargins): Boolean {
        if (cropMargins != this.margins) {
            this.margins = cropMargins
            return true
        }

        return false
    }


    override fun init(info: LastPageInfo, options: PageOptions) {
        changeCropMargins(CropMargins(info.leftMargin, info.rightMargin, info.topMargin, info.bottomMargin, info.leftEvenMargin, info.rightEventMargin, info.enableEvenCropping, info.cropMode))
        changeRotation(info.rotation)
        changeZoom(info.zoom)
        changeWalkOrder(info.walkOrder)
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
        info.walkOrder = walker.order.name
        info.pageLayout = layout
    }

    override fun convertToPoint(pos: LayoutPosition): Point {
        return Point(pos.x.marginLeft + pos.x.offset, pos.y.marginLeft + pos.y.offset)
    }

    override fun setViewSceneDimension(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    companion object {

        fun create(): SimpleLayoutStrategy {
            return SimpleLayoutStrategy()
        }
    }
}