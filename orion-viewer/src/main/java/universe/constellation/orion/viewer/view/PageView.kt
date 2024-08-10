package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LayoutData
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.timing
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min


enum class PageState(val interactWithUUI: Boolean) {
    STUB(false),
    CALC_GEOMETRY(false),
    SIZE_AND_BITMAP_CREATED(true),
    DESTROYED(false)
}

class PageView(
    pageNum: Int,
    document: Document,
    controller: Controller,
    rootJob: Job,
    val pageLayoutManager: PageLayoutManager
): CorePageView(pageNum, document, controller, rootJob) {

    val layoutData: LayoutData = LayoutData().apply {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    internal var scene: OrionDrawScene? = null


    @Volatile
    var bitmap: FlexibleBitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: PageState = PageState.STUB

    @Volatile
    var pageInfo: PageInfo? = null

    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val layoutInfo: LayoutPosition = LayoutPosition(pageNumber = pageNum)

    @Volatile
    lateinit var pageInfoJob: Job
        private set

    @Volatile
    var lastMainRenderingJob: Job? = null
        private set


    fun init() {
       reinit("init", Operation.DEFAULT)
    }

    fun destroy() {
        log("Page view $pageNum: destroying")
        state = PageState.DESTROYED
        cancelChildJobs(allJobs = true)
        bitmap?.disableAll(controller.bitmapCache)

        controller.scope.launch {
            waitJobsCancellation(allJobs = true)
            freePagePointer()
        }
    }

    private fun freePagePointer() {
        page.destroy()
    }

    fun reinit(marker: String = "reinit", operation: Operation = Operation.DEFAULT) {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        log("Page $pageNum $marker $state $document" )
        cancelChildJobs()
        if (::pageInfoJob.isInitialized) {
            pageInfoJob.cancel()
        }

        pageInfo = null
        pageInfoJob = dataPageScope.launch(Dispatchers.Main) {
            val info = getPageInfo(controller.layoutStrategy as SimpleLayoutStrategy)
            if (isActive) {
                controller.layoutStrategy.reset(layoutInfo, info)
                initBitmap(layoutInfo, info, operation)
            }
        }
    }

    private fun initBitmap(layoutInfo: LayoutPosition, info: PageInfo, operation: Operation) {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        val oldSize = Rect(wholePageRect)
        wholePageRect.set(0, 0, layoutInfo.x.pageDimension, layoutInfo.y.pageDimension)
        bitmap = bitmap?.resize(wholePageRect.width(), wholePageRect.height(), controller.bitmapCache)
            ?: pageLayoutManager.bitmapManager.createDefaultBitmap(wholePageRect.width(), wholePageRect.height(), pageNum)
        log("PageView.initBitmap $pageNum ${controller.document} $wholePageRect")
        pageInfo = info
        setInitialLayoutAndXPosition(operation, state)
        state = PageState.SIZE_AND_BITMAP_CREATED
        pageLayoutManager.onPageSizeCalculated(this, oldSize, info, operation)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        canvas.save()
        try {
            canvas.translate(layoutData.position.x, layoutData.position.y)
            if (state == PageState.SIZE_AND_BITMAP_CREATED && bitmap != null) {
                //draw bitmap
                log("Draw page $pageNum in state $state ${bitmap?.width} ${bitmap?.height} ")
                draw(canvas, bitmap!!, scene.defaultPaint!!, scene)
            } else {
                log("Draw border $pageNum in state $state")
                drawBlankLoadingPage(canvas, scene)
                drawBorder(canvas, scene)
            }
        } finally {
            canvas.restore()
        }
    }

    internal fun precacheData() {
        readRawSizeFromUI()
        readPageDataFromUI()
    }

    fun visibleRect(): Rect? {
        return layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)
    }

    private fun setInitialLayoutAndXPosition(operation: Operation, oldState: PageState) {
        val pageWidth = wholePageRect.width()
        val sceneWidth = pageLayoutManager.sceneRect.width()

        if (pageWidth <= sceneWidth) {
            layoutData.position.x = (sceneWidth - pageWidth) / 2.0f
        } else {
            if (operation != Operation.PINCH_ZOOM || oldState == PageState.STUB) {
                layoutData.position.x = -layoutInfo.x.offset.toFloat()
            }
        }
    }

    internal fun renderVisible(): Job? {
        if (!isOnScreen) {
            log("Non visible $pageNum");
            lastMainRenderingJob = null
        } else {
            lastMainRenderingJob = renderingScopeOnUI.launch {
                layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)?.let {
                    render(it, true, "Render visible")
                }
            }
        }
        return lastMainRenderingJob
    }

    fun renderInvisible(rect: Rect, tag: String, joinJob: Job?) {
        //TODO yield
        if (Rect.intersects(rect, wholePageRect)) {
            renderingScopeOnUI.launch {
                joinJob?.join()
                render(rect, false, "Render invisible $tag")
            }
        }
    }

    private suspend fun render(rect: Rect, fromUI: Boolean, tag: String) {
        readPageDataFromUI().fastJoin()

        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return

        if (state != PageState.SIZE_AND_BITMAP_CREATED) {
            return
        }
        //val bound = tempRegion.bounds
        val bound = Rect(rect)
        val bitmap = bitmap!!

        withContext(renderingScope.coroutineContext) {
            timing("$tag $pageNum page in rendering engine: $bound") {
                bitmap.render(bound, layoutInfo, page)
            }
        }

        if (coroutineContext.isActive) {
            if (fromUI) {
                log("PageView ($tag) invalidate: $pageNum $layoutData ${scene != null}")
                with(pageLayoutManager) {
                    if (this@PageView.isActivePage) {
                        scene.invalidate()
                    }
                }
            }
        } else {
            log("PageView.render $pageNum $layoutData: canceled")
        }
    }

    private val drawTmp  = Rect()
    private val drawTmpF  = RectF()
    private val sceneTmpF  = RectF()

    private fun draw(canvas: Canvas, bitmap: FlexibleBitmap, defaultPaint: Paint, scene: OrionDrawScene) {
        drawBlankLoadingPage(canvas, scene)
        bitmap.draw(canvas, calcDrawRect(scene) ?: return, defaultPaint)
        drawBorder(canvas, scene)
        scene.runAdditionalTaskInPageCanvasAndCoord(canvas, pageNum)
    }

    private fun drawBlankLoadingPage(
        canvas: Canvas,
        scene: OrionDrawScene
    ) {
        val pageRect = layoutData.wholePageRect
        canvas.drawRect(
            pageRect,
            scene.stuff.blankPagePaint
        )
        val size = min(pageRect.width(), pageRect.height()) / 10
        scene.loadingDrawable.setBounds(
            pageRect.centerX() - size / 2,
            pageRect.centerY() - size / 2,
            pageRect.centerX() + size / 2,
            pageRect.centerY() + size / 2
        )
        scene.loadingDrawable.draw(canvas)
    }

    private fun calcDrawRect(scene: OrionDrawScene): Rect? {
        if (scene.inScalingMode && scene.scale < 1f)
            return layoutData.wholePageRect
        else {
            sceneTmpF.set(pageLayoutManager.sceneRect)
            sceneTmpF.offset(-layoutData.position.x, -layoutData.position.y)

            drawTmpF.set(layoutData.wholePageRect)
            if (drawTmpF.intersect(sceneTmpF)) {
                drawTmp.set(
                    floor(drawTmpF.left).toInt(),
                    floor(drawTmpF.top).toInt(),
                    ceil(drawTmpF.right).toInt(),
                    ceil(drawTmpF.bottom).toInt()
                )
                return drawTmp
            }
        }
        return null
    }

    private fun drawBorder(
        canvas: Canvas,
        scene: OrionDrawScene,
    ) {
        if (scene.inScalingMode || controller.drawBorder.value) {
            canvas.drawRect(
                layoutData.wholePageRect,
                scene.borderPaint!!
            )
        }
    }

    fun invalidateAndUpdate() {
        invalidateAndDecreaseToCalcGeom()
        reinit(operation = Operation.DEFAULT)
    }

    fun invalidateAndDecreaseToCalcGeom() {
        if (state != PageState.STUB) {
            state = PageState.CALC_GEOMETRY
        }
        cancelChildJobs()
    }

    fun getSceneRect(pageRect: RectF): RectF {
        val rect = android.graphics.RectF(pageRect)
        val layoutInfo = layoutInfo
        val zoom  = layoutInfo.docZoom
        rect.zoom(zoom.toFloat())
        rect.offset(-layoutInfo.x.marginLeft.toFloat(), -layoutInfo.y.marginLeft.toFloat())
        rect.offset(layoutData.position)
        return rect
    }

}

val PageView.isOnScreen
    get() = pageLayoutManager.isVisible(this)

val PageView.height: Int
    get() = wholePageRect.height()

val PageView.width: Int
    get() = wholePageRect.width()

val PageView.wholePageRect
    get() = layoutData.wholePageRect

val PageView.pageEndY: Float
    get() = layoutData.position.y + wholePageRect.height()

//suspend fun <T> Deferred<T>.getOrAwait(): T {
//    if (isCompleted)  {
//        coroutineContext.ensureActive()
//    } else {
//
//    }
//}
//
//
suspend fun <T> Deferred<T>.fastJoin() {
    if (isCompleted)  {
        coroutineContext.ensureActive()
    } else {
        join()
    }
}
