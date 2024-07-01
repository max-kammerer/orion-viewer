package universe.constellation.orion.viewer.selection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val sin15 = sin(15.0/180.0*Math.PI).toFloat()

private val cos15 = cos(15.0/180.0*Math.PI).toFloat()

private val sqrt6 = sqrt(6.0).toFloat()

class SelectionViewNew : View {

    private val paint = Paint()

    private var rects: List<RectF> = emptyList()

    var startHandler: Handler? = null
        private set

    var endHandler: Handler? = null
        private set

    private val start = Path()

    private val end = Path()

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
        paint.alpha = 96
        rects.forEach {
            canvas.drawRect(it, paint)
        }

        paint.alpha = 128
        startHandler?.let { handler ->
            canvas.drawPath(handlerPath(handler), paint)
        }

        endHandler?.let { handler ->
            canvas.drawPath(handlerPath(handler), paint)
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

    private fun handlerPath(handler: Handler): Path {
        val path = if (handler.isStart) start else end
        path.reset()
        val x = handler.x
        val y = handler.y
        val r = if (handler.isStart) handler.triangleSize else -handler.triangleSize
        path.moveTo(x, y)
        path.lineTo(x - r * sin15, y - r * cos15)
        path.lineTo(x - r * cos15, y - r * sin15)
        path.lineTo(x, y)
        path.close()
        return path
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
        paint.color = 0x1d897f
        paint.colorFilter = colorFilter
        paint.alpha = 64
        paint.strokeWidth = 2f
    }
}

data class Handler(var x: Float, var y: Float, var triangleSize: Float, val isStart: Boolean)

fun SelectionViewNew.findClosestHandler(x: Float, y: Float, trashHold: Float): Handler? {
    val min = listOfNotNull(
        startHandler to startHandler?.distance(x, y),
        endHandler to endHandler?.distance(x, y)
    ).minByOrNull { it.second ?: Float.MAX_VALUE } ?: return null
    return min.takeIf { trashHold >= (it.second ?: Float.MAX_VALUE) }?.first
}

fun Handler.distance(x: Float, y: Float): Float {
    var delta = - triangleSize * sqrt6 / 4 / 2
    if (!isStart) delta = -delta
    val x1 = this.x + delta
    val y1 = this.y + delta

    val dx = x1 - x
    val dy = y1 - y
    return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}
