package universe.constellation.orion.viewer.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.view.ColorStuff

/**
 * Created by mike on 27.10.15.
 */
class PageView(val pageNum: Int) {

    private val stuffTempRect = Rect()

    var bitmap: Bitmap? = null;

    var position: LayoutPosition? = null;

    fun draw(canvas: Canvas, colors: ColorStuff) {
        initRect()

        bitmap?.let {
            canvas.drawBitmap(bitmap, stuffTempRect, stuffTempRect, colors.bd.paint)
        }
    }

    fun drawBorder(canvas: Canvas, colors: ColorStuff) {
        position?.let {
            Common.d("Draw: border")

            val right = -it.x.offset + it.x.pageDimension
            val bottom = -it.y.offset + it.y.pageDimension

            canvas.drawRect(
                    (-it.x.offset).toFloat(),
                    (-it.y.offset).toFloat(),
                    right.toFloat(),
                    bottom.toFloat(),
                    colors.borderPaint
            )
        }
    }

    private fun initRect() {
        position?.let {
            stuffTempRect.set(
                    it.x.occupiedAreaStart,
                    it.y.occupiedAreaStart,
                    it.x.occupiedAreaEnd,
                    it.y.occupiedAreaEnd);
        }
    }
}