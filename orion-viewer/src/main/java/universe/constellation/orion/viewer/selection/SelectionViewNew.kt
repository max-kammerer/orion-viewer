package universe.constellation.orion.viewer.selection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt

class SelectionViewNew : View {

    private val paint = Paint()

    private var rects: List<RectF> = emptyList()

    var startHandler: Handler? = null
        private set

    var endHandler: Handler? = null
        private set

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.alpha = 64
        rects.forEach {
            canvas.drawRect(it, paint)
        }

        paint.alpha = 96
        startHandler?.let { handler ->
            canvas.drawCircle(handler.x, handler.y, handler.radius, paint)
        }

        endHandler?.let { handler ->
            canvas.drawCircle(handler.x, handler.y, handler.radius, paint)
        }

        startHandler?.let { startHandler ->
            endHandler?.let { endHandler ->
                val left = minOf(startHandler.x, endHandler.x)
                val top = minOf(startHandler.y, endHandler.y)
                val right = maxOf(startHandler.x, endHandler.x)
                val bottom = maxOf(startHandler.y, endHandler.y)

                paint.style = Paint.Style.STROKE
                paint.alpha = 64
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }

    }

    fun updateView(rect: RectF) {
        updateView(listOf(rect))
    }

    fun setHandlers(startHandler: Handler, endHandler: Handler) {
        this.startHandler = startHandler
        this.endHandler = endHandler
        println("" + startHandler + endHandler)
    }

    fun updateView(rects: List<RectF>) {
        this.rects = rects
        invalidate()
    }

    fun reset() {
        rects = emptyList()
        startHandler = null
        endHandler = null
    }

    fun setColorFilter(colorFilter: ColorFilter?) {
        paint.color = Color.BLACK
        paint.colorFilter = colorFilter
        paint.alpha = 64
        paint.strokeWidth = 2f
    }
}

data class Handler(var x: Float, var y: Float, var radius: Float, val isStart: Boolean)

fun SelectionViewNew.findClosestHandler(x: Float, y: Float, trashHold: Float): Handler? {
    val min = listOfNotNull(
        startHandler to startHandler?.distance(x, y),
        endHandler to endHandler?.distance(x, y)
    ).minByOrNull { it.second ?: Float.MAX_VALUE } ?: return null
    return min.takeIf { trashHold >= (it.second ?: Float.MAX_VALUE) }?.first
}

fun Handler.distance(x: Float, y: Float): Float {
    val dx = this.x - x
    val dy = this.y - y
    return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}
