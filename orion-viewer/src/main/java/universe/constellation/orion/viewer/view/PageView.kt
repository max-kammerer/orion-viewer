package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LayoutData
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.timing
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min


enum class PageState(val interactWithUUI: Boolean) {
    STUB(false),
    SIZE_AND_BITMAP_CREATED(true),
    DESTROYED(false)
}

class PageView(
    val pageNum: Int,
    val document: Document,
    val controller: Controller,
    val rootJob: Job,
    val pageLayoutManager: PageLayoutManager
) {
    private val analytics = pageLayoutManager.controller.activity.analytics

    init {
        if (pageNum < 0) errorInDebug("Invalid page number: $pageNum")
    }

    private val handler = CoroutineExceptionHandler { _, ex ->
        errorInDebug("Processing error for page $pageNum", ex)
        analytics.error(ex)
    }

    val layoutData: LayoutData = LayoutData().apply {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val pageEndY: Float
        get() = layoutData.position.y + wholePageRect.height()

    val wholePageRect
        get() = layoutData.wholePageRect

    val isOnScreen
        get() = pageLayoutManager.isVisible(this)

    val width: Int
        get() = wholePageRect.width()
    val height: Int
        get() = wholePageRect.height()

    internal var scene: OrionDrawScene? = null

    @Volatile
    var isVisibleState = false

    internal var pageJobs = SupervisorJob(rootJob)

    @Volatile
    var bitmap: FlexibleBitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: PageState = PageState.STUB

    internal val page = document.getOrCreatePageAdapter(pageNum)

    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val layoutInfo: LayoutPosition = LayoutPosition(pageNumber = pageNum)

    @Volatile
    lateinit var pageInfo: Deferred<PageInfo>

    fun init() {
       reinit("init")
    }

    fun toInvisible() {
        //TODO optimize canceling state
        if (isVisibleState) {
            log("PV: toInvisible $pageNum")
            //state = PageState.CAN_BE_DELETED
            //pageJobs.cancelChildren()
            isVisibleState = false
        }
    }

    fun toVisible() {
        //TODO optimize canceling state
        if (!isVisibleState) {
            log("PV: to visible $pageNum")
            isVisibleState = true
        }
    }

    fun destroy() {
        log("Page view $pageNum: destroying")
        toInvisible()
        state = PageState.DESTROYED
        pageJobs.cancel()
        bitmap?.disableAll(controller.bitmapCache)
        bitmap = null
        controller.scope.launch {
            pageJobs.cancelAndJoin()
            freePagePointer()
        }
    }

    private fun freePagePointer() {
        page.destroy()
    }

    fun reinit(marker: String = "reinit") {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        log("Page $pageNum $marker $state $document" )
        pageJobs.cancelChildren()
        pageInfo = GlobalScope.async(controller.context + pageJobs + handler) {
            val info = page.getPageInfo(controller.layoutStrategy as SimpleLayoutStrategy)
            if (isActive) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        controller.layoutStrategy.reset(layoutInfo, info)
                        initBitmap(layoutInfo)
                    }
                }
            }
            info
        }
    }

    private fun initBitmap(layoutInfo: LayoutPosition) {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        val oldSize = Rect(wholePageRect)
        wholePageRect.set(0, 0, layoutInfo.x.pageDimension, layoutInfo.y.pageDimension)
        bitmap = bitmap?.resize(wholePageRect.width(), wholePageRect.height(), controller.bitmapCache)
            ?: pageLayoutManager.bitmapManager.createDefaultBitmap(wholePageRect.width(), wholePageRect.height(), pageNum)
        log("PageView.initBitmap $pageNum ${controller.document} $wholePageRect")
        state = PageState.SIZE_AND_BITMAP_CREATED
        pageLayoutManager.onPageSizeCalculated(this, oldSize)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        if (state != PageState.STUB && bitmap!= null) {
            //draw bitmap
            log("Draw page $pageNum in state $state ${bitmap?.width} ${bitmap?.height} ")
            draw(canvas, bitmap!!, scene.defaultPaint!!, scene)
        } else {
            log("Draw border $pageNum in state $state")
            drawBorder(canvas, scene)
        }
    }

    internal fun renderVisibleAsync() {
        GlobalScope.launch (Dispatchers.Main + pageJobs + handler) {
            renderVisible()
        }
    }

    internal fun precacheData() {
        GlobalScope.async(controller.context + pageJobs + handler) {
            page.getPageSize()
            if (isActive) {
                page.readPageDataForRendering()
            }
        }
    }

    fun visibleRect(): Rect? {
        return layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)
    }

    internal suspend fun renderVisible() {
        if (!isOnScreen) {
            log("Non visible $pageNum");
            return
        }

        coroutineScope {
            launch (Dispatchers.Main + pageJobs + handler) {
                layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)?.let {
                    render(it, true)
                }
            }/*.join()*/
        }
    }

    internal suspend fun renderInvisible(rect: Rect) {
        //TODO yield
        if (Rect.intersects(rect, wholePageRect)) {
            render(rect, false)
        }
    }

    private suspend fun render(rect: Rect, fromUI: Boolean) {
        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return

        if (state != PageState.SIZE_AND_BITMAP_CREATED) {
            pageInfo.await()
        }
        //val bound = tempRegion.bounds
        val bound = Rect(rect)
        val bitmap = bitmap!!

        coroutineScope {
            launch(controller.context + pageJobs + handler) {
                timing("Rendering $pageNum page in rendering engine: $bound") {
                    bitmap.render(bound, layoutInfo, page)
                }
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        if (kotlin.coroutines.coroutineContext.isActive) {
                            log("PageView.render invalidate: $pageNum $layoutData ${scene != null}")
                            scene?.invalidate()
                            if (fromUI) {
                                precache()
                            }
                        }
                    }
                } else {
                    log("PageView.render $pageNum $layoutData: canceled")
                }
            }/*.join()*/
        }
    }

    private val drawTmp  = Rect()
    private val drawTmpF  = RectF()
    private val sceneTmpF  = RectF()

    private fun draw(canvas: Canvas, bitmap: FlexibleBitmap, defaultPaint: Paint, scene: OrionDrawScene) {
        canvas.save()
        try {
            canvas.translate(layoutData.position.x, layoutData.position.y)
            drawBlankLoadingPage(canvas, scene)

            bitmap.draw(canvas, calcDrawRect(scene) ?: return, defaultPaint)

            drawBorder(canvas, scene)

            scene.runAdditionalTaskInPageCanvasAndCoord(canvas, pageNum)
        } finally {
            canvas.restore()
        }
    }

    private fun drawBlankLoadingPage(
        canvas: Canvas,
        scene: OrionDrawScene
    ) {
        val pageRect = layoutData.wholePageRect
        canvas.drawRect(
            pageRect,
            scene.stuff.pagePaint
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
        canvas.drawRect(
            layoutData.wholePageRect,
            scene.borderPaint!!
        )
    }

    fun invalidateAndUpdate() {
        invalidateAndMoveToStub()
        reinit()
    }

    fun invalidateAndMoveToStub() {
        state = PageState.STUB
        pageJobs.cancelChildren()
    }
}