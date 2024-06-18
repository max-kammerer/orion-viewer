package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.util.MoveUtil


class PageScene : View {

    private val visibleRect = Rect()

    init {
        viewTreeObserver.addOnScrollChangedListener {
            if (pageView != null) {
                triggerPaint()
            }
        }

    }

    private fun triggerPaint() {
        if (getGlobalVisibleRect(visibleRect)) {
            log("PageView.triggerPaint " + pageView?.pageNum + ": " + visibleRect)
        }
        if (getLocalVisibleRect(visibleRect)) {
            pageView?.renderVisible()
        }
    }

    private lateinit var statusBar: StatusBar

    private var pageView: PageView? = null
        set(value) {
            field = value
            triggerPaint()
        }

    internal var scale = 1.0f

    private var startFocus: PointF? = null

    private var endFocus: PointF? = null

    private var enableMoveOnPinchZoom: Boolean = false

    internal var borderPaint: Paint? = null

    internal var defaultPaint: Paint? = null

    internal var inScaling = false

    private val tasks = ArrayList<DrawTask>()

    private var inited = false

    private lateinit var stuff: ColorStuff

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun init(colorStuff: ColorStuff, statusBar: StatusBar) {
        this.stuff = colorStuff
        defaultPaint = colorStuff.backgroundPaint
        borderPaint = colorStuff.borderPaint
        this.statusBar = statusBar
        inited = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        log("Scene onDraw: ${pageView?.pageNum}")
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

        //pageView?.draw(canvas, this)

        if (inScaling) {
            canvas.restore()
        }

        for (drawTask in tasks) {
            drawTask.drawOnCanvas(canvas, stuff, null)
        }
        canvas.restore()

    }


    fun doScale(scale: Float, startFocus: PointF, endFocus: PointF, enableMoveOnPinchZoom: Boolean) {
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
}
