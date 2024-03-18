package universe.constellation.orion.viewer.view

import android.graphics.Canvas

interface DrawTask {
    fun drawOnCanvas(canvas: Canvas, stuff: ColorStuff, drawContext: DrawContext?)

    fun accept(page: Int) = false
}
