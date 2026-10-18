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
import android.os.SystemClock
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.Scroller
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.util.MoveUtil

class OrionDrawScene : View {

    internal lateinit var loadingDrawable: Drawable
        private set

    internal lateinit var statusBar: StatusBar
        private set

    private var dimensionAware: ViewDimensionAware? = null

    var pageLayoutManager: PageLayoutManager? = null

    internal var scale = 1.0f

    private var startFocus: PointF? = null

    private var endFocus: PointF? = null

    private var enableMoveOnPinchZoom: Boolean = false

    internal var borderPaint: Paint? = null

    internal var defaultPaint: Paint? = null

    internal var inScalingMode = false
        private set

    private val tasks = ArrayList<DrawTask>()

    private var inited = false

    /*
     * The last frame of a pinch, shown until the pages are rendered at the new zoom: after the
     * gesture they are laid out again and their tiles freed, so they would be blank meanwhile.
     */
    private var frozenFrame: Bitmap? = null
    private var frozenUntil = 0L

    lateinit var stuff: ColorStuff

    val scroller = Scroller(context)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun init(colorStuff: ColorStuff, statusBarHelper: StatusBar, loadingDrawable: Drawable) {
        this.stuff = colorStuff
        defaultPaint = colorStuff.mainPagePaint
        borderPaint = colorStuff.borderPaint
        this.statusBar = statusBarHelper
        this.loadingDrawable = loadingDrawable
        inited = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!inited) {
            return
        }
        val frame = frozenFrameToShow()

        canvas.save()
        val myScale = scale

        if (inScalingMode) {
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

        if (frame != null) {
            //a pinch over a frozen frame scales the frame as it would the pages
            canvas.drawBitmap(frame, 0f, 0f, null)
        } else {
            pageLayoutManager?.renderVisiblePages(canvas, this)
        }

        if (inScalingMode) {
            canvas.restore()
        }

        //TODO move to page
        canvas.restore()
    }

    fun runAdditionalTaskInPageCanvasAndCoord(canvas: Canvas, page: Int) {
        for (drawTask in tasks) {
            if (drawTask.accept(page)) {
                drawTask.drawOnCanvas(canvas, stuff, null)
            }
        }
    }

    fun setDimensionAware(dimensionAware: ViewDimensionAware?) {
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

    fun doScale(scale: Float, startFocus: PointF, endFocus: PointF, enableMoveOnPinchZoom: Boolean) {
        this.scale = scale
        this.startFocus = startFocus
        this.endFocus = endFocus
        this.enableMoveOnPinchZoom = enableMoveOnPinchZoom
    }

    fun enableScalingMode() {
        inScalingMode = true
    }

    /**
     * The frozen frame while the visible pages aren't rendered yet, otherwise null, the frame
     * dropped. A pinch started over it keeps it however long it lasts: it is what the gesture
     * scales, the pages behind it may be blank.
     */
    private fun frozenFrameToShow(): Bitmap? {
        val frame = frozenFrame ?: return null
        val manager = pageLayoutManager
        if (manager != null && !manager.areVisiblePagesRendered() &&
            (inScalingMode || SystemClock.uptimeMillis() < frozenUntil)
        ) {
            return frame
        }
        frozenFrame = null
        return null
    }

    /**
     * Keeps showing what is on the screen now, until the visible pages are rendered again or
     * [FREEZE_LIMIT_MS] pass: call right before the layout changes under a finished pinch.
     */
    fun freezeFrame() {
        if (width <= 0 || height <= 0) return
        val frame = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return
        }
        //over the previous frozen frame, if the pages are still being rendered after an earlier pinch
        draw(Canvas(frame))
        frozenFrame = frame
        frozenUntil = SystemClock.uptimeMillis() + FREEZE_LIMIT_MS
        /* Right away: until the scene draws again, the system may redraw its last recorded
           frame, which refers to the tile bitmaps that the new layout is about to clear and
           hand to other tiles. */
        invalidate()
        //redraws when the limit passes, should the rendering never finish
        postInvalidateDelayed(FREEZE_LIMIT_MS)
    }

    /** Back to the live pages: the user moved them, the frame is out of date. */
    fun unfreeze() {
        if (frozenFrame == null) return
        frozenFrame = null
        invalidate()
    }

    fun disableScalingMode() {
        this.inScalingMode = false
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

private const val FREEZE_LIMIT_MS = 1500L
