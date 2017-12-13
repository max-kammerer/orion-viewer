package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.layout.LayoutPosition
import java.util.concurrent.CountDownLatch

/**
 * Created by mike on 24.10.15.
 */
interface Scene : OrionImageListener {
    fun setColorMatrix(colorMatrix: FloatArray?) {
    }

    fun setDrawOffPage(drawOffPage: Boolean) {
    }
}

class FullScene(val scene: ViewGroup, val drawView: OrionScene, statusBar: ViewGroup, val context: Context) : Scene, OrionBookListener {

    val statusBarHelper = OrionStatusBarHelper(statusBar)

    val colorStuff = ColorStuff(context)

    init {
        drawView.init(colorStuff)
    }

    override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
        drawView.onNewImage(bitmap, info, latch)
        statusBarHelper.onNewImage(bitmap, info, latch)
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