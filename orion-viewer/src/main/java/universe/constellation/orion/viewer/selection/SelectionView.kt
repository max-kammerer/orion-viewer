package universe.constellation.orion.viewer.selection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class SelectionView : View {
    private var oldRect: Rect? = null

    private val paint = Paint()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (oldRect != null) {
            canvas.drawRect(oldRect!!, paint)
        }
    }

    fun updateView(left: Int, top: Int, right: Int, bottom: Int) {
        val newRect = Rect(left, top, right, bottom)
        val invalidate = Rect(newRect)
        if (oldRect != null) {
            invalidate.union(oldRect!!)
        }
        oldRect = newRect

        invalidate(invalidate)
    }

    fun reset() {
        oldRect = null
    }

    fun setColorFilter(colorFilter: ColorFilter?) {
        paint.color = Color.BLACK
        paint.colorFilter = colorFilter
        paint.alpha = 64
    }
}
