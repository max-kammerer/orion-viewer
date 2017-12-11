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

package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

import java.util.ArrayList
import java.util.concurrent.CountDownLatch

import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.OrionScene
import universe.constellation.orion.viewer.util.MoveUtil

class OrionDrawScene : View, OrionScene {

    var bitmap: Bitmap? = null

    override var info: LayoutPosition? = null

    private var latch: CountDownLatch? = null

    private var dimensionAware: ViewDimensionAware? = null

    private var scale = 1.0f

    private var startFocus: Point? = null

    private var endFocus: Point? = null

    private var enableMoveOnPinchZoom: Boolean = false

    private var borderPaint: Paint? = null

    private var defaultPaint: Paint? = null

    private var inScaling = false

    private val tasks = ArrayList<DrawTask>()

    private val stuffTempRect = Rect()

    private var inited = false

    private lateinit var stuff: ColorStuff

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun init(colorStuff: ColorStuff) {
        this.stuff = colorStuff
        defaultPaint = colorStuff.bd.paint
        borderPaint = colorStuff.borderPaint
        inited = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!inited) {
            return
        }

        canvas.save()
        canvas.translate(0f, 0f)
        if (bitmap != null && !bitmap!!.isRecycled) {
            val start = System.currentTimeMillis()
            Common.d("OrionView: drawing bitmap on view...")

            val myScale = scale

            if (inScaling) {
                Common.d("in scaling")
                canvas.save()
                canvas.translate(
                        -MoveUtil.calcOffset(startFocus!!.x, endFocus!!.x, myScale, enableMoveOnPinchZoom),
                        -MoveUtil.calcOffset(startFocus!!.y, endFocus!!.y, myScale, enableMoveOnPinchZoom))
                canvas.scale(myScale, myScale)
            }

            stuffTempRect.set(
                    info!!.x.occupiedAreaStart,
                    info!!.y.occupiedAreaStart,
                    info!!.x.occupiedAreaEnd,
                    info!!.y.occupiedAreaEnd)

            canvas.drawBitmap(bitmap!!, stuffTempRect, stuffTempRect, defaultPaint)

            if (inScaling) {
                canvas.restore()
                drawBorder(canvas, myScale)
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s")

            for (drawTask in tasks) {
                drawTask.drawOnCanvas(canvas, stuff, null)
            }
        }
        canvas.restore()

        if (latch != null) {
            latch!!.countDown()
        }
    }

    private fun drawBorder(canvas: Canvas, myScale: Float) {
        Common.d("Draw: border")

        val left = ((-info!!.x.offset - startFocus!!.x) * myScale + if (enableMoveOnPinchZoom) endFocus!!.x else startFocus!!.x).toInt()
        val top = ((-info!!.y.offset - startFocus!!.y) * myScale + if (enableMoveOnPinchZoom) endFocus!!.y else startFocus!!.y).toInt()

        val right = (left + info!!.x.pageDimension * myScale).toInt()
        val bottom = (top + info!!.y.pageDimension * myScale).toInt()

        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), borderPaint!!)
    }

    override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
        this.bitmap = bitmap
        this.latch = latch
        this.info = info
    }

    override fun setDimensionAware(dimensionAware: ViewDimensionAware) {
        this.dimensionAware = dimensionAware
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Common.d("OrionView: onSizeChanged " + w + "x" + h)
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            if (dimensionAware != null) {
                dimensionAware!!.onDimensionChanged(width, height)
            }
        }
    }


    override fun isDefaultColorMatrix(): Boolean {
        return defaultPaint!!.colorFilter == null
    }

    override fun doScale(scale: Float, startFocus: Point, endFocus: Point, enableMoveOnPinchZoom: Boolean) {
        this.scale = scale
        this.startFocus = startFocus
        this.endFocus = endFocus
        this.enableMoveOnPinchZoom = enableMoveOnPinchZoom
    }

    override fun beforeScaling() {
        inScaling = true
    }

    override fun afterScaling() {
        this.inScaling = false
    }

    override fun addTask(drawTask: DrawTask) {
        tasks.add(drawTask)
    }

    override fun removeTask(drawTask: DrawTask) {
        tasks.remove(drawTask)
    }

    override fun toView(): View {
        return this
    }

    override val sceneWidth: Int
        get() = width
    override val sceneHeight: Int
        get() = height
}
