package universe.constellation.orion.viewer.selection

import android.content.Context
import android.graphics.Canvas
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

        paint.alpha = 64

        if (oldRect != null) {
            //System.out.println("Draw rect " + oldRect);
            canvas.drawRect(oldRect!!, paint)
        }
    }

    fun updateView(left: Int, top: Int, right: Int, bottom: Int) {
        //System.out.println("updateView");
        val newRect = Rect(left, top, right, bottom)
        val invalidate = Rect(newRect)
        if (oldRect != null) {
            invalidate.union(oldRect!!)
        }
        oldRect = newRect

        //postInvalidateDelayed(30, invalidate.left, invalidate.top, invalidate.right, invalidate.bottom);
        invalidate(invalidate)
    }

    fun reset() {
        oldRect = null
    }
}
