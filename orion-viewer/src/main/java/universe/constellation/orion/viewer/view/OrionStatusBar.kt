package universe.constellation.orion.viewer.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.layout.LayoutPosition
import java.util.concurrent.CountDownLatch

class OrionStatusBarHelper(val view: ViewGroup) : OrionBookListener, OrionImageListener {
    val panel = view.findViewById<View>(R.id.orion_status_bar) as ViewGroup
    val title = view.findViewById<View>(R.id.title) as TextView
    val offset = view.findViewById<View>(R.id.offset) as TextView
    val page = view.findViewById<View>(R.id.page) as TextView
    val totalPages = view.findViewById<View>(R.id.totalPages) as TextView
    var info: LayoutPosition? = null

    override fun onNewBook(title: String?, pageCount: Int) {
        this.title.text = title
        this.totalPages.text = "/$pageCount"
        this.page.text = "?"
        this.offset.text = "[?, ?]"
    }

    @SuppressLint("SetTextI18n")
    override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
        info?.let {
            offset.text = "[${pad(info.x.offset)}:${pad(info.y.offset)}]"
            page.text = "${info.pageNumber + 1}"
        }
    }

    private fun pad(value: Int): String {
        val pValue = Math.abs(value)
        return when {
            pValue < 10 -> "  $value"
            pValue < 100 -> " $value"
            else -> "$value"
        }
    }

    fun setShowOffset(showOffset: Boolean) {
        offset.visibility = if (showOffset) View.VISIBLE else View.GONE
    }

    fun setShowStatusBar(showStatusBar: Boolean) {
        panel.visibility = if (showStatusBar) View.VISIBLE else View.GONE
    }

    fun setColorMatrix(colorMatrix: FloatArray?) {
        val colorFilter = if (colorMatrix == null) null else ColorMatrixColorFilter(colorMatrix)
        (0 until panel.childCount).forEach {
            val child = panel.getChildAt(it)
            when(child) {
                is TextView -> child.paint.colorFilter = colorFilter
            }
        }
    }

}