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

package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.PageWalker.*
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.OneDimension

class PageWalker(val order: WALK_ORDER) {

    enum class DIR constructor(val delta: Int) {
        //X, Y, -X, -Y
        X(1), Y(1), X_M(-1), Y_M(-1);

        fun inverse() =
                when (this) {
                    X -> X_M
                    X_M -> X
                    Y -> Y_M
                    Y_M -> Y
                }

        fun isX() = this === X_M || this === X

        fun toLeftOrUp() = this === X_M || this === Y_M

        fun toRightOrDown() = this === X || this === Y
    }

    enum class WALK_ORDER constructor(val code: Int, val first: DIR, val second: DIR) {

        ABCD(0, DIR.X, DIR.Y), ACBD(1, DIR.Y, DIR.X), BADC(2, DIR.X_M, DIR.Y), BDAC(3, DIR.Y, DIR.X_M);

        fun code(): String = name

        val isLeftToRight: Boolean
            get() = this === ABCD || this === ACBD
    }

    private val doCentering = true

    fun next(info: LayoutPosition, layout: Int): Boolean {
        return next(info, order.first, order.second, layout)
    }

    fun prev(info: LayoutPosition, layout: Int): Boolean {
        return next(info, order.first.inverse(), order.second.inverse(), layout)
    }

    //true if should show prev/next page
    private fun next(info: LayoutPosition, firstDir: DIR, secondDir: DIR, layout: Int): Boolean {
        val isX = firstDir.isX()
        val first = if (isX) info.x else info.y
        val second = if (isX) info.y else info.x

        var changeSecond = true
        var newFirstOffset = -1
        var newSecondOffset = -1

        for (i in 1..2) {
            val dimension = if (i == 1) first else second
            var dir = if (i == 1) firstDir else secondDir

            val inverseX = dir.isX() && !order.isLeftToRight
            var offset = if (inverseX) dimension.pageDimension - dimension.offset - dimension.screenDimension else dimension.offset
            dir = if (inverseX) dir.inverse() else dir

            if (changeSecond) {
                changeSecond = false

                val newOffsetStart = offset + dimension.screenDimension * dir.delta
                val newOffsetEnd = newOffsetStart + dimension.screenDimension - 1

                if (!inInterval(newOffsetStart, dimension) && !inInterval(newOffsetEnd, dimension)) {
                    changeSecond = true
                    offset = reset(dir, dimension, true, layout)
                } else {
                    if (needAlign(dir, layout) && (!inInterval(newOffsetStart, dimension) || !inInterval(newOffsetEnd, dimension))) {
                        offset = align(dir, dimension)
                    } else {
                        offset = newOffsetStart - dimension.overlap * dir.delta
                    }
                }
            }

            offset = if (inverseX) dimension.pageDimension - offset - dimension.screenDimension else offset

            if (i == 1) {
                newFirstOffset = offset
            } else {
                newSecondOffset = offset
            }
        }

        if (!changeSecond) {
            (if (isX) info.x else info.y).offset = newFirstOffset
            second.offset = newSecondOffset
        }

        return changeSecond
    }

    private fun reset(dir: DIR, dim: OneDimension, doCentering: Boolean, layout: Int): Int {
        if (this.doCentering && doCentering) {
            if (dim.pageDimension < dim.screenDimension) {
                return (dim.pageDimension - dim.screenDimension) / 2
            }
        }

        if (dir.toRightOrDown() || dim.pageDimension <= dim.screenDimension || dim.screenDimension == 0) {
            return 0
        } else {
            if (!needAlign(dir, layout)) {
                return (dim.pageDimension - dim.overlap) / (dim.screenDimension - dim.overlap) * (dim.screenDimension - dim.overlap)
            } else {
                return dim.pageDimension - dim.screenDimension
            }
        }
    }


    private fun align(dir: DIR, dim: OneDimension): Int {
        if (dir.toRightOrDown()) {
            return dim.pageDimension - dim.screenDimension
        } else {
            return 0
        }
    }

    private fun needAlign(dir: DIR, layout: Int): Boolean {
        return dir.isX() && layout != 0 || !dir.isX() && layout == 1
    }

    private fun inInterval(value: Int, dim: OneDimension): Boolean {
        return value >= 0 && value < dim.pageDimension
    }


    fun reset(info: LayoutPosition, isNext: Boolean, doCentering: Boolean, layout: Int) {
        val first = if (isNext) order.first else order.first.inverse()
        val second = if (isNext) order.second else order.second.inverse()

        var horDir = if (first.isX()) first else second
        val vertDir = if (first.isX()) second else first

        val inverse = !order.isLeftToRight
        horDir = if (inverse) horDir.inverse() else horDir
        info.x.offset = reset(horDir, info.x, doCentering, layout)
        info.x.offset = if (inverse) info.x.pageDimension - info.x.screenDimension - info.x.offset else info.x.offset

        info.y.offset = reset(vertDir, info.y, doCentering, layout)
    }

}

val String.walkOrder: WALK_ORDER
    get() = WALK_ORDER.values().firstOrNull {
        it.code() == this
    } ?: WALK_ORDER.ABCD