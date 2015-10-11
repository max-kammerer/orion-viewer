package universe.constellation.orion.viewer.view

import android.content.Context
import android.view.ViewGroup

/**
 * Created by mike on 24.10.15.
 */
public class FullScene(val scene: ViewGroup, val drawView: OrionDrawScene, statusaBar: ViewGroup, val context: Context) {

    val statusBarHelper = OrionStatusBarHelper(statusaBar)

    val colorStuff = ColorStuff(context)

    init {
        drawView.init(colorStuff)
    }

    fun onNewBook(title: String, pageCount: Int) {
        drawView.onNewBook(title, pageCount)
        statusBarHelper.onNewBook(title, pageCount)
    }

    fun setColorMatrix(colorMatrix: FloatArray?) {
        colorStuff.setColorMatrix(scene, colorMatrix)
        statusBarHelper.setColorMatrix(colorMatrix)
    }

    fun setDrawOffPage(drawOffPage: Boolean) {
        colorStuff.renderOffPage(scene, drawOffPage)
    }
}