package universe.constellation.orion.viewer.view

import android.annotation.SuppressLint
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.document.abs

class OrionStatusBarHelper(val view: ViewGroup) : OrionBookListener {

    private val panel = view.findViewById<View>(R.id.orion_status_bar) as ViewGroup
    private val title = view.findViewById<View>(R.id.title) as TextView
    private val offset = view.findViewById<View>(R.id.offset) as TextView
    private val page = view.findViewById<View>(R.id.page) as TextView
    private val totalPages = view.findViewById<View>(R.id.totalPages) as TextView

    @SuppressLint("SetTextI18n")
    override fun onNewBook(title: String?, pageCount: Int) {
        this.title.text = title
        this.totalPages.text = "/$pageCount"
        this.page.text = "?"
        this.offset.text = "[?, ?]"
    }

    @SuppressLint("SetTextI18n")
    fun onPageUpdate(pageNum: Int, x: Int, y: Int) {
        offset.text = "[${pad(x)}:${pad(y)}]"
        page.text = "${pageNum + 1}"
    }

    private fun pad(value: Int): String {
        val pValue = abs(value)
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
            if (child is TextView) child.paint.colorFilter = colorFilter
        }
    }

}