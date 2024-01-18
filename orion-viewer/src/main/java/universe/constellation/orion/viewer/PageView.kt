package universe.constellation.orion.viewer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.PageLayoutManager
import kotlin.math.max

enum class State(val interactWithUUI: Boolean) {
    CREATED(false),
    SIZE_AND_BITMAP_CREATED(true),
    CAN_BE_DELETED(false),
    DESTROYED(false)
}

interface PageInitListener {

    fun onPageInited(pageView: PageView)
}

class LayoutData {
    var order: Int = 0
    val position: PointF = PointF(0f, 0f)
    val wholePageRect: Rect = Rect()
    val tmpRect: Rect = Rect()

    val globalLeft: Float
        get() = position.x + wholePageRect.left

    val globalRight: Float
        get() = position.x + wholePageRect.right

    companion object {
        val EMPTY = Rect()
    }

    fun contains(x: Float, y: Float): Boolean {
        return wholePageRect.contains((x - position.x) .toInt(), (y-position.y).toInt())
    }

    fun visibleOnScreenPart(screenRect: Rect): Rect {
        tmpRect.set(wholePageRect)
        tmpRect.offset(position.x.toInt(), position.y.toInt())
        if (tmpRect.intersect(screenRect)) {
            return tmpRect
        } else {
            return EMPTY
        }
    }

    fun insideScreenX(screenRect: Rect): Boolean {
        return wholePageRect.left >= screenRect.left && wholePageRect.right <= screenRect.right
    }

    override fun toString(): String {
        return "LayoutData(order=$order, position=$position, viewDimension=$wholePageRect)"
    }

}

class PageView(
    val pageNum: Int,
    val document: Document,
    val controller: Controller,
    val rootJob: Job,
    val pageLayoutManager: PageLayoutManager
) {

    val layoutData: LayoutData = LayoutData().apply {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val offset: Position = Point(0, 0)

    val wholePageRect
        get() = layoutData.wholePageRect

    val width: Int
        get() = wholePageRect.width()
    val height: Int
        get() = wholePageRect.height()

    internal var scene: OrionDrawScene? = null

    var pageJobs = SupervisorJob(rootJob)

    @Volatile
    var bitmap: Bitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: State = State.CREATED

    @Volatile
    var marker: Int = 1


    private val processingPagePart = Rect()
    private val nonRenderedRegion = Region()
    private val tempRegion = Region()
    private val visiblePart = Rect()
    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }


    val layoutInfo: LayoutPosition = LayoutPosition()

    private fun stopJobs() {

    }

    fun init() {
       reinit()
    }

    fun markAsDeattached() {
        //TODO optimize canceling state
        println("Deattached $pageNum")
        state = State.CAN_BE_DELETED
        pageJobs.cancelChildren()
        bitmap?.apply {
            controller.bitmapCache.free(this)
            bitmap = null
        }
    }

    fun destroy() {
        println("Destroy $pageNum")
        assert(state == State.CAN_BE_DELETED) {"Wrong state calling destroy: $state"}
        markAsDeattached()
        state = State.DESTROYED
        pageJobs.cancel()
        GlobalScope.launch(controller.context) {
            pageJobs.cancelAndJoin()
            freePagePointer()
        }
    }

    private fun freePagePointer() {
        
    }

    fun toInvisibleState() {
        markAsDeattached()
    }

    fun reinit() {
        if (state == State.SIZE_AND_BITMAP_CREATED) return
        println("Page $pageNum reinit $state $document" )
        //wholePageRect.set(pageLayoutManager.defaultSize())
        //layoutData.wholePageRect.set(wholePageRect)

        state = State.CREATED
        pageJobs.cancelChildren()
        val myMarker = marker
        GlobalScope.launch(controller.context + pageJobs) {
            controller.layoutStrategy.reset(layoutInfo, pageNum)
            if (isActive) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        initBitmap(layoutInfo)
                    }
                }
            }
        }
    }

    private fun initBitmap(layoutInfo: LayoutPosition) {
        if (state == State.SIZE_AND_BITMAP_CREATED) return
        val oldSize = Rect(wholePageRect)
        wholePageRect.set(0, 0, layoutInfo.x.pageDimension, layoutInfo.y.pageDimension)
        processingPagePart.set(wholePageRect) //TODO
        nonRenderedRegion.set(processingPagePart)
        bitmap = bitmap?.let {
            if (!it.isRecycled) {
                if (!(it.width >= wholePageRect.width() && it.height >= wholePageRect.height())) {
                    controller.bitmapCache.free(it)
                    null
                } else {
                    it
                }
            } else {
                null
            }
        } ?: createDefaultBitmap()
        log("initBitmap $pageNum ${controller.document}: $nonRenderedRegion")
        state = State.SIZE_AND_BITMAP_CREATED
        pageLayoutManager.onSizeCalculated(this, oldSize)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        if (state != State.CREATED && bitmap!= null && bitmap?.isRecycled != true) {
            //draw bitmap
            println("Draw page $pageNum in state $state ${bitmap?.isRecycled} ${bitmap?.width} ${bitmap?.height} ")
            draw(canvas, bitmap!!, layoutInfo, scene.defaultPaint!!, scene)
        } else {
            println("Draw border $pageNum in state $state: ${layoutData} on screen ${layoutData.visibleOnScreenPart(scene.pageLayoutManager!!.sceneRect)}")
            drawBorder(canvas, scene, layoutInfo)
        }
        scene.orionStatusBarHelper.onPageUpdate(layoutInfo)
    }

    //TODO scale up to necessary, tune to heap size
    private fun createDefaultBitmap(): android.graphics.Bitmap {
        val width = max(wholePageRect.width(), controller.layoutStrategy.viewWidth)
        val height = max(wholePageRect.height(), controller.layoutStrategy.viewHeight)
        return controller.bitmapCache.createBitmap(width, height)
    }

    internal fun renderVisible(uiCallaback: Function1<Any, Unit>? = null) {
        val tmpRect = layoutData.tmpRect
        tmpRect.set(wholePageRect)
        tmpRect.offset(layoutData.position.x.toInt(), layoutData.position.y.toInt())

        if (pageLayoutManager.isVisible(this)) {
            tmpRect.offset((-layoutData.position.x).toInt(), (-layoutData.position.y).toInt())
            render(tmpRect, uiCallaback)
        }
    }

    internal fun render(rect: Rect, uiCallaback: Function1<Any, Unit>? = null) {
        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return

        val handler = CoroutineExceptionHandler { _, _ ->
            log("Bitmap rendering cancelled")
            //TODO processing
        }

        println("Before check $pageNum: $nonRenderedRegion")
        println("Before check to draw $pageNum: $rect")
        tempRegion.set(nonRenderedRegion)
        if (tempRegion.op(rect, nonRenderedRegion, Region.Op.INTERSECT)) {
            val bound = tempRegion.bounds
            GlobalScope.launch (controller.context + pageJobs + handler) {
                timing("Rendering $pageNum page in rendering engine: $bound") {
                    renderInner(bound, layoutInfo, pageNum, document, bitmap!!, layoutStrategy)
                }
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        if (kotlin.coroutines.coroutineContext.isActive) {
                            nonRenderedRegion.op(bound, Region.Op.DIFFERENCE)
                            log("new nonrender $pageNum: " + nonRenderedRegion.toString())
                            log("invalidate: $pageNum $layoutData")
                            println("inv: $pageNum ")
                            scene?.invalidate()
                            if (uiCallaback != null) {
                                uiCallaback(bitmap!!)
                            }
                        }
                    }
                }
            }
        } else {
            scene?.invalidate()
            if (uiCallaback != null) {
                uiCallaback(bitmap!!)
            }
        }
    }

    private fun draw(canvas: Canvas, bitmap: Bitmap, info: LayoutPosition, defaultPaint: Paint, scene: OrionDrawScene) {
        println("DrawX $pageNum:  $bitmap")
        canvas.save()
        if (!bitmap.isRecycled) {
            val tmpRect  = Rect()
            val screenRect  = RectF(pageLayoutManager.sceneRect)
            tmpRect.set(wholePageRect)
            tmpRect.offset(layoutData.position.x.toInt(), layoutData.position.y.toInt())
            if (tmpRect.intersect(pageLayoutManager.sceneRect)) {
                screenRect.set(tmpRect)
                tmpRect.offset(-layoutData.position.x.toInt(), -layoutData.position.y.toInt())
                //tmpRect2.offset(layoutData.position.x.toFloat(), layoutData.position.y.toFloat())
                println("Draw $pageNum:  $tmpRect $screenRect $bitmap")
                canvas.drawBitmap(bitmap, tmpRect, screenRect, defaultPaint)
            }else {
                println("Skip $tmpRect $screenRect")
            }
            drawBorder(canvas, scene, info)
        } else {
            println("Recycled: " + bitmap.isRecycled)
        }
        canvas.restore()
    }

    private fun drawBorder(
        canvas: Canvas,
        scene: OrionDrawScene,
        info: LayoutPosition
    ) {
        //if (scene.inScaling) {
            //log("Draw: border $info")
        canvas.save()
        canvas.translate(layoutData.position.x.toFloat(), layoutData.position.y.toFloat())
            val left = 0
            val top = 0

            val right = (info.x.pageDimension )
            val bottom = (info.y.pageDimension )

            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat(),
                scene.borderPaint!!
            )
        canvas.restore()
        //}
    }

    fun free() {
        freeBitmap()
    }

    private fun freeBitmap() {
        bitmap?.let {
            controller.bitmapCache.free(it)
        }
    }

    fun invalidateAndUpdate() {
        marker++
        state = State.CREATED
        pageJobs.cancelChildren()
        reinit()
    }
}