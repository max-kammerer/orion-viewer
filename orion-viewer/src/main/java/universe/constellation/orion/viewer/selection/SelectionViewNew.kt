package universe.constellation.orion.viewer.selection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SelectionViewNew : View {

    private val paint = Paint()

    private var rects: List<RectF> = emptyList()

    private var startHandler: Handler? = null

    private var endHandler: Handler? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rects.forEach {
            canvas.drawRect(it, paint)
        }

        startHandler?.let { handler ->
            canvas.drawCircle(handler.x, handler.y, handler.radius, paint)
        }

        endHandler?.let { handler ->
            canvas.drawCircle(handler.x, handler.y, handler.radius, paint)
        }
    }

    fun updateView(rects: RectF) {
        updateView(listOf(rects))
    }

    fun updateView(rects: List<RectF>, startHandler: Handler? = null, endHandler: Handler? = null) {
        this.rects = rects
        this.startHandler = startHandler
        this.endHandler = endHandler
        invalidate()
    }

    fun reset() {
        rects = emptyList()
    }

    fun setColorFilter(colorFilter: ColorFilter?) {
        paint.color = Color.BLACK
        paint.colorFilter = colorFilter
        paint.alpha = 64
    }
}

class Handler(var x: Float, var y: Float, var radius: Float)