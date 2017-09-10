package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Point
import android.view.View
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.DrawTask
import universe.constellation.orion.viewer.view.ViewDimensionAware

import java.util.concurrent.CountDownLatch

/**
 * User: mike
 * Date: 20.10.13
 * Time: 9:21
 */
interface OrionBookListener {
    fun onNewBook(title: String?, pageCount: Int)
}

interface OrionImageListener {
    fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?)
}

interface OrionScene : OrionImageListener {

    fun init(colorStuff: ColorStuff)

    fun doScale(scale: Float, startFocus: Point, endFocus: Point, enableMoveOnPinchZoom: Boolean)

    fun beforeScaling()

    fun afterScaling()

    fun postInvalidate()

    fun invalidate()

    val sceneWidth: Int

    val sceneHeight: Int

    val info: LayoutPosition?

    fun setDimensionAware(dimensionAware: ViewDimensionAware)

    fun setOnTouchListener(listener: View.OnTouchListener)

    fun toView(): View

    fun addTask(drawTask: DrawTask)

    fun removeTask(drawTask: DrawTask)

    fun isDefaultColorMatrix(): Boolean
}