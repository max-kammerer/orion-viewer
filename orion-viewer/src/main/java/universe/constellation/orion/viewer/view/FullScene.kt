package universe.constellation.orion.viewer.view

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.OrionImageView
import java.util.concurrent.CountDownLatch

/**
 * Created by mike on 24.10.15.
 */
public class FullScene(val scene: ViewGroup, val drawView: OrionDrawScene, statusaBar: ViewGroup, val context: Context) : OrionImageView {

    val statusBarHelper = OrionStatusBarHelper(statusaBar)

    val colorStuff = ColorStuff(context)

    init {
        drawView.init(colorStuff)
    }

    override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
        drawView.onNewImage(bitmap, info, latch)
        statusBarHelper.onNewImage(bitmap, info, latch)
    }

    override fun onNewBook(title: String, pageCount: Int) {
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