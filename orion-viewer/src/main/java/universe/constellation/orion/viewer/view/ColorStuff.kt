package universe.constellation.orion.viewer.view

import android.graphics.*
import android.graphics.Paint.FILTER_BITMAP_FLAG
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import universe.constellation.orion.viewer.util.ColorUtil

class ColorStuff {

    val borderPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 0f
        style = Paint.Style.STROKE
    }

    val backgroundPaint = Paint(FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    val pagePaint = Paint(FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    private var colorDrawable = ColorDrawable(backgroundPaint.color)
    private var renderOffPage: Boolean = false
    private var transformationArray: FloatArray? = null

    fun setColorMatrix(view: View, colorMatrix: FloatArray?) {
        transformationArray = colorMatrix
        val filter = if (colorMatrix != null) ColorMatrixColorFilter(ColorMatrix(colorMatrix)) else null
        backgroundPaint.colorFilter = filter
        colorDrawable.colorFilter = filter
        borderPaint.colorFilter = filter
        pagePaint.colorFilter = filter
        renderOffPage(view, renderOffPage)
    }

    fun renderOffPage(view: View, on: Boolean) {
        renderOffPage = on
        backgroundPaint.color = if (on) Color.rgb(230, 230, 230) else Color.WHITE
        colorDrawable.color = backgroundPaint.color
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && transformationArray != null) {
            //ugly hack
            colorDrawable.color = ColorUtil.transformColor(colorDrawable.color, transformationArray!!)
        }
        ViewCompat.setBackground(view, colorDrawable)
    }
}
