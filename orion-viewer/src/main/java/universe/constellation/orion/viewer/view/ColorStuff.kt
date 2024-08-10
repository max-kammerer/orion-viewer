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

    val mainPagePaint = Paint(FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    val blankPagePaint = Paint(FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    private var colorDrawable = ColorDrawable(mainPagePaint.color)
    private var renderOffPage: Boolean = false

    var colorMatrix: FloatArray? = null
        private set

    fun setColorMatrix(view: View, colorMatrix: FloatArray?) {
        this.colorMatrix = colorMatrix
        val filter = if (colorMatrix != null) ColorMatrixColorFilter(ColorMatrix(colorMatrix)) else null
        mainPagePaint.colorFilter = filter
        colorDrawable.colorFilter = filter
        borderPaint.colorFilter = filter
        blankPagePaint.colorFilter = filter
        renderOffPage(view, renderOffPage)
    }

    fun renderOffPage(view: View, on: Boolean) {
        renderOffPage = on
        mainPagePaint.color = if (on) Color.rgb(230, 230, 230) else Color.WHITE
        colorDrawable.color = mainPagePaint.color
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && colorMatrix != null) {
            //ugly hack
            colorDrawable.color = ColorUtil.transformColor(colorDrawable.color, colorMatrix!!)
        }
        ViewCompat.setBackground(view, colorDrawable)
    }
}
