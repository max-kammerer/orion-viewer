package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import universe.constellation.orion.viewer.OrionScene
import universe.constellation.orion.viewer.PageView
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.util.MoveUtil


class PageScene : View, OrionScene {

    val visibleRect = Rect()

    init {
        viewTreeObserver.addOnScrollChangedListener({
            if (pageView != null) {
                triggerPaint()
            }
        })

    }

    private fun triggerPaint() {
        if (getGlobalVisibleRect(visibleRect)) {
            println("Scroll global: " + pageView?.pageNum + " " + visibleRect)
        }
        if (getLocalVisibleRect(visibleRect)) {
            println("Scroll: " + pageView?.pageNum + " " + visibleRect)
            pageView?.render(visibleRect)
        }
    }

    internal lateinit var orionStatusBarHelper: OrionStatusBarHelper

    internal var pageView: PageView? = null
        set(value) {
            field = value
            triggerPaint()
        }

    override val info: LayoutPosition?
        get() = pageView?.layoutInfo

    private var dimensionAware: ViewDimensionAware? = null

    internal var scale = 1.0f

    private var startFocus: Point? = null

    private var endFocus: Point? = null

    private var enableMoveOnPinchZoom: Boolean = false

    internal var borderPaint: Paint? = null

    internal var defaultPaint: Paint? = null

    internal var inScaling = false

    private val tasks = ArrayList<DrawTask>()

    private var inited = false

    override var sceneRect = Rect(0, 0, 0, 0)

    private lateinit var stuff: ColorStuff

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun init(colorStuff: ColorStuff, orionStatusBarHelper: OrionStatusBarHelper) {
        this.stuff = colorStuff
        defaultPaint = colorStuff.backgroundPaint
        borderPaint = colorStuff.borderPaint
        this.orionStatusBarHelper = orionStatusBarHelper
        inited = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        println("Scene onDraw: ${pageView?.pageNum}")
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

    override fun setDimensionAware(dimensionAware: ViewDimensionAware) {
        this.dimensionAware = dimensionAware
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        log("PageScene: onSizeChanged " + w + "x" + h)
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            if (dimensionAware != null) {
                dimensionAware!!.onDimensionChanged(width, height)
                triggerPaint()
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

    override val sceneYLocationOnScreen: Int
        get() = IntArray(2).run { getLocationOnScreen(this); this[1] }
}
