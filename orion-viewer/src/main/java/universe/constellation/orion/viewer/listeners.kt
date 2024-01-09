package universe.constellation.orion.viewer

import android.graphics.Point
import android.view.View
import universe.constellation.orion.viewer.geometry.Rect
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.DrawTask
import universe.constellation.orion.viewer.view.OrionStatusBarHelper
import universe.constellation.orion.viewer.view.ViewDimensionAware

typealias Position = Point
typealias ORect = Rect

interface OrionBookListener {
    fun onNewBook(title: String?, pageCount: Int)
}

interface OrionScene {

    fun init(colorStuff: ColorStuff, statusBarHelper: OrionStatusBarHelper)

    fun doScale(scale: Float, startFocus: Point, endFocus: Point, enableMoveOnPinchZoom: Boolean)

    fun beforeScaling()

    fun afterScaling()

    fun postInvalidate()

    fun invalidate()

    val sceneWidth: Int

    val sceneHeight: Int

    val sceneYLocationOnScreen: Int

    val info: LayoutPosition?

    fun setDimensionAware(dimensionAware: ViewDimensionAware)

    fun setOnTouchListener(listener: View.OnTouchListener)

    fun toView(): View

    fun addTask(drawTask: DrawTask)

    fun removeTask(drawTask: DrawTask)

    fun isDefaultColorMatrix(): Boolean

    val sceneRect: android.graphics.Rect
}

