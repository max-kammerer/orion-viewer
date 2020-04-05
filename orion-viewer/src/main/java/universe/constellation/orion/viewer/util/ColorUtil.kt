package universe.constellation.orion.viewer.util

import android.graphics.Color
import android.graphics.ColorMatrix
import androidx.annotation.ColorInt

object ColorUtil {

    private val COLOR_MATRICES: Map<String, FloatArray?> = linkedMapOf(
            "CM_NORMAL" to null,

            "CM_INVERTED" to floatArrayOf(
                    -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ),

            "CM_BLACK_ON_YELLOWISH" to floatArrayOf(
                    0.94f, 0.02f, 0.02f, 0.0f, 0.0f,
                    0.02f, 0.86f, 0.02f, 0.0f, 0.0f,
                    0.02f, 0.02f, 0.74f, 0.0f, 0.0f,
                    0.00f, 0.00f, 0.00f, 1.0f, 0.0f
            ),

            "CM_GRAYSCALE_LIGHT" to floatArrayOf(
                    0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
                    0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
                    0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
                    0.00f, 0.00f, 0.00f, 1.0f, 0.0f
            ),


            "CM_GRAYSCALE" to floatArrayOf(
                    0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
                    0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
                    0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
                    0.000f, 0.00f, 0.00f, 1.0f, 0.0f
            ),

            "CM_GRAYSCALE_DARK" to floatArrayOf(
                    0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
                    0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
                    0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
                    0.00f, 0.00f, 0.00f, 1.0f, 0.0f
            ),

            "CM_WHITE_ON_BLUE" to floatArrayOf(
                    -0.94f, -0.02f, -0.02f, 1.0f, 1.0f,
                    -0.02f, -0.86f, -0.02f, 1.0f, 1.0f,
                    -0.02f, -0.02f, -0.74f, 1.0f, 1.0f,
                    0.00f, 0.00f, 0.00f, 1.0f, 0.0f
            )
    )

    @JvmStatic
    fun getColorMode(type: String?): FloatArray? = if (type == null) null else COLOR_MATRICES[type]

    @ColorInt
    @JvmStatic
    fun transformColor(color: Int, matrix: ColorMatrix): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val a = Color.alpha(color)

        val array = IntArray(4)
        val transformation = matrix.array
        for (i in array.indices) {
            val shift = i * 5
            array[i] = (
                            r * transformation[shift + 0] +
                            g * transformation[shift + 1] +
                            b * transformation[shift + 2] +
                            a * transformation[shift + 3] +
                                transformation[shift + 4]
                    ).toInt()
        }
        return Color.argb(array[3], array[0], array[1], array[2])
    }
}
