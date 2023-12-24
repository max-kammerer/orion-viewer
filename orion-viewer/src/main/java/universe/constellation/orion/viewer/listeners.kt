package universe.constellation.orion.viewer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.geometry.Rect
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.DrawTask
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.OrionStatusBarHelper
import universe.constellation.orion.viewer.view.ViewDimensionAware
import java.util.concurrent.CountDownLatch

typealias Position = Point
typealias ORect = Rect

interface OrionBookListener {
    fun onNewBook(title: String?, pageCount: Int)
}

class PageView(
    val pageNum0: Int,
    val document: Document,
    val scene: OrionScene,
    var position: Position = Position(0, 0),
    val layoutStrategy: LayoutStrategy,
    val controller: Controller,
) {

    var bitmap: Bitmap? = null
    lateinit var curPos: LayoutPosition
    private val stuffTempRect = android.graphics.Rect()

    var info = PageInfo(pageNum0, scene.sceneWidth, scene.sceneHeight).apply { getPageInfo() }

    fun getPageInfo() {
        //document.getPageInfo()
    }

    lateinit var rect: Rect

    fun layoutInfo(info: LayoutPosition) {
        if (!::curPos.isInitialized  || curPos != info) {
            bitmap?.let {
                controller.bitmapCache.free(it)
            }
        }
        curPos = info.deepCopy()
        GlobalScope.launch(Dispatchers.Main) {
            update(scene as OrionDrawScene)
        }
    }

    init {
        init()
    }

    fun init() {
        rect = ORect(0, 0, scene.sceneWidth, scene.sceneHeight)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        if (::curPos.isInitialized && bitmap!= null && bitmap?.isRecycled != true) {
            //draw bitmap
            draw(canvas, bitmap!!, curPos, scene.defaultPaint!!, scene)
        } else {
            drawBorder(canvas, scene, curPos)
        }
        scene.orionStatusBarHelper.onPageUpdate(curPos)
    }

    private suspend fun update(scene: OrionDrawScene) {
        val newBitmap = controller.bitmapCache.createBitmap(layoutStrategy.viewWidth, layoutStrategy.viewHeight)
        withContext(Dispatchers.Default) {
            timing("Rendering $pageNum0 page in rendering engine") {
                renderInner(curPos, document, newBitmap, layoutStrategy)
            }
        }

        bitmap = newBitmap

        scene.invalidate()
    }

    private fun draw(canvas: Canvas, bitmap: Bitmap, info: LayoutPosition, defaultPaint: Paint, scene: OrionDrawScene) {
        canvas.save()
        if (!bitmap.isRecycled) {
            stuffTempRect.set(
                info.x.occupiedAreaStart,
                info.y.occupiedAreaStart,
                info.x.occupiedAreaEnd,
                info.y.occupiedAreaEnd
            )

            canvas.drawBitmap(bitmap, stuffTempRect, stuffTempRect, defaultPaint)

            //TODO:
//            for (drawTask in tasks) {
//                drawTask.drawOnCanvas(canvas, stuff, null)
//            }
            drawBorder(canvas, scene, info)
        }
        canvas.restore()
    }

    private fun drawBorder(
        canvas: Canvas,
        scene: OrionDrawScene,
        info: LayoutPosition
    ) {
        if (scene.inScaling) {
            log("Draw: border")

            val left = (-info.x.offset)
            val top = (-info.y.offset)

            val right = (left + info.x.pageDimension )
            val bottom = (top + info.y.pageDimension )

            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat(),
                scene.borderPaint!!
            )
        }
    }

    fun free() {
        bitmap?.let {
            controller.bitmapCache.free(it)
        }
    }

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

    fun addPage(pageView: PageView)

    val sceneRect: android.graphics.Rect
}