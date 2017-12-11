package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.View
import universe.constellation.orion.viewer.L
import universe.constellation.orion.viewer.util.ColorUtil
import universe.constellation.orion.viewer.util.DensityUtil

class ColorStuff(context: Context) {

    val backgroundPaint = Paint()

    val borderPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    val bd: BitmapDrawable
    private var colorDrawable = DrawableCompat.wrap(ColorDrawable(Color.WHITE))
    private var renderOffPage: Boolean = false

    init {
        val dim = 64
        val bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val p = Paint()
        p.color = Color.rgb(223, 223, 223)
        val gradsize = (1 shl (Math.log(DensityUtil.calcScreenSize(2, context)) / Math.log(2.0) + 0.1).toInt()).let {
            if (it < 2) 2 else it
        }

        L.log("Grad size is $gradsize")
        p.shader = LinearGradient(0f, 0f, 0f, gradsize.toFloat(), Color.rgb(223, 223, 223), Color.rgb(240, 240, 240), Shader.TileMode.MIRROR)
        canvas.drawRect(0f, 0f, dim.toFloat(), dim.toFloat(), p)

        val bitmapShader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        backgroundPaint.shader = bitmapShader
        bd = BitmapDrawable(context.resources, bitmap)
        bd.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

    }

    fun setColorMatrix(view: View, colorMatrix: FloatArray?) {
        if (colorMatrix != null) {
            val matrix = ColorMatrix(colorMatrix)
            val filter = ColorMatrixColorFilter(matrix)
            bd.colorFilter = filter
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //ugly hack
                colorDrawable = ColorDrawable(ColorUtil.transformColor(Color.WHITE, matrix))
            }
            colorDrawable.colorFilter = filter
            borderPaint.colorFilter = filter
        } else {
            bd.colorFilter = null
            colorDrawable.colorFilter = null
            borderPaint.colorFilter = null
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //ugly hack
                colorDrawable = ColorDrawable(Color.WHITE)
            }
        }
        renderOffPage(view, renderOffPage)
    }

    fun renderOffPage(view: View, on: Boolean) {
        renderOffPage = on
        val currentPaint = if (on) bd else colorDrawable
        view.setBackgroundDrawable(currentPaint)
    }
}
