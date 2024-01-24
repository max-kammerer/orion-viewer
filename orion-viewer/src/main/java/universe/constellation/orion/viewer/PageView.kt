package universe.constellation.orion.viewer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.PageLayoutManager
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap

enum class State(val interactWithUUI: Boolean) {
    STUB(false),
    SIZE_AND_BITMAP_CREATED(true),
    CAN_BE_DELETED(false),
    DESTROYED(false)
}

interface PageInitListener {

    fun onPageInited(pageView: PageView)
}

val handler = CoroutineExceptionHandler { _, ex ->
    log("Bitmap rendering cancelled")
    ex.printStackTrace()
    //TODO processing
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


    fun contains(x: Float, y: Float): Boolean {
        return wholePageRect.contains((x - position.x) .toInt(), (y-position.y).toInt())
    }

    fun globalRectInTmp(): Rect {
        tmpRect.set(wholePageRect)
        tmpRect.offset(position.x.toInt(), position.y.toInt())
        return tmpRect
    }

    fun occupiedScreenPartInTmp(screenRect: Rect): Rect? {
        val globalPosition = globalRectInTmp()
        return if (globalPosition.intersect(screenRect)) {
            globalPosition
        } else {
            null
        }
    }

    fun visibleOnScreenPartInTmp(screenRect: Rect): Rect? {
        val occupiedScreenPart = occupiedScreenPartInTmp(screenRect) ?: return null
        occupiedScreenPart.offset(-position.x.toInt(), -position.y.toInt())
        return occupiedScreenPart
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

    val wholePageRect
        get() = layoutData.wholePageRect

    val width: Int
        get() = wholePageRect.width()
    val height: Int
        get() = wholePageRect.height()

    internal var scene: OrionDrawScene? = null

    private var pageJobs = SupervisorJob(rootJob)
    private var pageInfoJob: Deferred<LayoutPosition>? = null

    @Volatile
    var bitmap: FlexibleBitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: State = State.STUB

    @Volatile
    var marker: Int = 1


    private val processingPagePart = Rect()
    private val nonRenderedRegion = Region()
    private val tempRegion = Region()
    private val visiblePart = Rect()
    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }




    private val renderingScope = CoroutineScope(controller.context + pageJobs + handler)
    private val pageInfoScope = CoroutineScope(controller.context + pageJobs + handler)

    val layoutInfo: LayoutPosition = LayoutPosition()

    @Volatile
    lateinit var pageInfo: Deferred<PageView>

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
            this.free(controller.bitmapCache)
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
        pageJobs.cancelChildren()
        val myMarker = marker
        pageInfo = GlobalScope.async(controller.context + pageJobs + handler) {
            controller.layoutStrategy.reset(layoutInfo, pageNum)
            if (isActive) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        initBitmap(layoutInfo)
                    }
                }
            }
            this@PageView
        }
    }

    private fun initBitmap(layoutInfo: LayoutPosition) {
        if (state == State.SIZE_AND_BITMAP_CREATED) return
        val oldSize = Rect(wholePageRect)
        wholePageRect.set(0, 0, layoutInfo.x.pageDimension, layoutInfo.y.pageDimension)
        nonRenderedRegion.set(wholePageRect)
        bitmap = bitmap?.resize(wholePageRect.width(), wholePageRect.height(), controller.bitmapCache)
            ?: createDefaultBitmap()
        log("PageView.initBitmap $pageNum ${controller.document}: $nonRenderedRegion")
        state = State.SIZE_AND_BITMAP_CREATED
        pageLayoutManager.onSizeCalculated(this, oldSize)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        if (state != State.STUB && bitmap!= null) {
            //draw bitmap
            println("Draw page $pageNum in state $state ${bitmap?.width} ${bitmap?.height} ")
            draw(canvas, bitmap!!, scene.defaultPaint!!, scene)
        } else {
            println("Draw border $pageNum in state $state: ${layoutData} on screen ${layoutData.occupiedScreenPartInTmp(scene.pageLayoutManager!!.sceneRect)}")
            drawBorder(canvas, scene)
        }
        scene.orionStatusBarHelper.onPageUpdate(layoutInfo)
    }

    //TODO scale up to necessary, tune to heap size
    private fun createDefaultBitmap(): FlexibleBitmap {
        val deviceInfo = controller.getDeviceInfo()
        return FlexibleBitmap(wholePageRect, deviceInfo.width / 2, deviceInfo.height / 2)
    }

    internal fun renderVisibleAsync() {
        GlobalScope.async (Dispatchers.Main + handler) {
            renderVisible()
        }
    }

    internal suspend fun renderVisible(): Deferred<PageView?>? {
        return layoutData.visibleOnScreenPartInTmp(pageLayoutManager.sceneRect)?.let {
            return coroutineScope {
                async (Dispatchers.Main + handler) {
                    render(it)?.await()
                }
            }
        } ?: run { println("Non visible $pageNum"); null }

    }

    internal suspend fun render(rect: Rect): Deferred<PageView>? {
        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return null

        if (state != State.SIZE_AND_BITMAP_CREATED) {
            pageInfo.await()
        }
        tempRegion.set(nonRenderedRegion)
        if (tempRegion.op(rect, nonRenderedRegion, Region.Op.INTERSECT)) {
            val bound = tempRegion.bounds
            return coroutineScope {
                async(controller.context + pageJobs + handler) {
                    timing("Rendering $pageNum page in rendering engine: $bound") {
                        bitmap!!.render(bound, layoutInfo, pageNum, document, controller.bitmapCache)
                    }
                    if (isActive) {
                        withContext(Dispatchers.Main) {
                            if (kotlin.coroutines.coroutineContext.isActive) {
                                nonRenderedRegion.op(bound, Region.Op.DIFFERENCE)
                                log("PageView.render invalidate: $pageNum $layoutData")
                                scene?.invalidate()
                            }
                        }
                    } else {
                        log("PageView.render: canceled")
                    }
                    this@PageView
                }
            }
        } else {
            if (state == State.SIZE_AND_BITMAP_CREATED) {
                println("Already rendered $state $document $pageNum: $rect")
                scene?.invalidate()
                val completableDeferred = CompletableDeferred<PageView>(coroutineContext.job)
                completableDeferred.complete(this@PageView)
                return completableDeferred
            } else {
                println("Skipped $state $document $pageNum")
            }
        }
        return null
    }

    private val drawTmp  = Rect()
    private val drawSceneRect  = RectF()

    private fun draw(canvas: Canvas, bitmap: FlexibleBitmap, defaultPaint: Paint, scene: OrionDrawScene) {
        canvas.save()
        drawTmp.set(wholePageRect)
        drawTmp.offset(layoutData.position.x.toInt(), layoutData.position.y.toInt())
        if (drawTmp.intersect(pageLayoutManager.sceneRect)) {
            drawSceneRect.set(drawTmp)
            drawTmp.offset(-layoutData.position.x.toInt(), -layoutData.position.y.toInt())
            println("PageView.draw: $pageNum:  $drawTmp $drawSceneRect ${bitmap.width * bitmap.height}")
            bitmap.draw(canvas, drawTmp, drawSceneRect, defaultPaint, scene.borderPaint!!)
        } else {
            println("PageView.draw: skipped $drawTmp $drawSceneRect")
        }
        drawBorder(canvas, scene)

        canvas.restore()
    }

    private fun drawBorder(
        canvas: Canvas,
        scene: OrionDrawScene,
    ) {
        canvas.drawRect(
            layoutData.globalRectInTmp(),
            scene.borderPaint!!
        )
    }

    private fun i(): Int {
        val bottom = wholePageRect.height()
        return bottom
    }

    fun invalidateAndUpdate() {
        marker++
        state = State.STUB
        pageJobs.cancelChildren()
        reinit()
    }
}