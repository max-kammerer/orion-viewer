package universe.constellation.orion.viewer.view

import android.content.Context
import android.view.ViewGroup
import universe.constellation.orion.viewer.OrionBookListener
import universe.constellation.orion.viewer.OrionScene

/**
 * Created by mike on 24.10.15.
 */
interface Scene  {
    fun setColorMatrix(colorMatrix: FloatArray?) {
    }

    fun setDrawOffPage(drawOffPage: Boolean) {
    }
}

class FullScene(private val scene: ViewGroup, val drawView: OrionScene, statusBar: ViewGroup, val context: Context) : Scene, OrionBookListener {

    val statusBarHelper = OrionStatusBarHelper(statusBar)

    private val colorStuff = ColorStuff()

    init {
        drawView.init(colorStuff)
    }

    override fun onNewBook(title: String?, pageCount: Int) {
        statusBarHelper.onNewBook(title, pageCount)
    }

    override fun setColorMatrix(colorMatrix: FloatArray?) {
        colorStuff.setColorMatrix(scene, colorMatrix)
        statusBarHelper.setColorMatrix(colorMatrix)
    }

    override fun setDrawOffPage(drawOffPage: Boolean) {
        colorStuff.renderOffPage(scene, drawOffPage)
    }
}