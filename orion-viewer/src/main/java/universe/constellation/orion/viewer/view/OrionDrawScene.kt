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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import universe.constellation.orion.viewer.PageView
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.util.MoveUtil

class OrionDrawScene : View {

    internal lateinit var orionStatusBarHelper: OrionStatusBarHelper

    internal var pageView: PageView? = null

    private var dimensionAware: ViewDimensionAware? = null

    var pageLayoutManager: PageLayoutManager? = null

    internal var scale = 1.0f

    private var startFocus: Point? = null

    private var endFocus: Point? = null

    private var enableMoveOnPinchZoom: Boolean = false

    internal var borderPaint: Paint? = null

    internal var defaultPaint: Paint? = null

    internal var inScaling = false

    private val tasks = ArrayList<DrawTask>()

    private var inited = false

    var sceneRect = Rect(0, 0, 0, 0)

    private lateinit var stuff: ColorStuff

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun init(colorStuff: ColorStuff, statusBarHelper: OrionStatusBarHelper) {
        this.stuff = colorStuff
        defaultPaint = colorStuff.backgroundPaint
        borderPaint = colorStuff.borderPaint
        this.orionStatusBarHelper = statusBarHelper
        inited = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!inited) {
            return
        }

        canvas.save()
        val myScale = scale

        if (inScaling) {
            log("in scaling")
            canvas.save()
            canvas.translate(
                -MoveUtil.calcOffset(
                    startFocus!!.x,
                    endFocus!!.x,
                    myScale,
                    enableMoveOnPinchZoom
                ),
                -MoveUtil.calcOffset(
                    startFocus!!.y,
                    endFocus!!.y,
                    myScale,
                    enableMoveOnPinchZoom
                )
            )
            canvas.scale(myScale, myScale)
        }
        for (p in pageLayoutManager?.visiblePages ?: emptyList()) {
            p.draw(canvas, this)
        }

        if (inScaling) {
            canvas.restore()
        }

        //TODO move to page
        for (drawTask in tasks) {
            drawTask.drawOnCanvas(canvas, stuff, null)
        }
        canvas.restore()

    }

    fun setDimensionAware(dimensionAware: ViewDimensionAware) {
        this.dimensionAware = dimensionAware
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        log("OrionView: onSizeChanged " + w + "x" + h)
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            if (dimensionAware != null) {
                dimensionAware!!.onDimensionChanged(width, height)
            }
        }
    }

    fun isDefaultColorMatrix(): Boolean {
        return defaultPaint!!.colorFilter == null
    }

    fun doScale(scale: Float, startFocus: Point, endFocus: Point, enableMoveOnPinchZoom: Boolean) {
        this.scale = scale
        this.startFocus = startFocus
        this.endFocus = endFocus
        this.enableMoveOnPinchZoom = enableMoveOnPinchZoom
    }

    fun beforeScaling() {
        inScaling = true
    }

    fun afterScaling() {
        this.inScaling = false
    }

    fun addTask(drawTask: DrawTask) {
        tasks.add(drawTask)
    }

    fun removeTask(drawTask: DrawTask) {
        tasks.remove(drawTask)
    }

    fun toView(): View {
        return this
    }

    val sceneWidth: Int
        get() = width
    val sceneHeight: Int
        get() = height

    val sceneYLocationOnScreen: Int
        get() = IntArray(2).run { getLocationOnScreen(this); this[1] }
}
