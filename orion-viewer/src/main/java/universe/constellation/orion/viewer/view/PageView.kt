package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LayoutData
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.PageSize
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

    private val handler = CoroutineExceptionHandler { _, ex ->
        errorInDebug("Processing error for page $pageNum", ex)
        analytics.error(ex)
    }

    val layoutData: LayoutData = LayoutData().apply {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    internal var scene: OrionDrawScene? = null

    private val renderingPageJobs = SupervisorJob(rootJob)

    private val dataPageJobs = SupervisorJob(rootJob)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val renderingScope = CoroutineScope(
        controller.renderingDispatcher.limitedParallelism(2) + renderingPageJobs + handler
    )

    private val dataPageScope = CoroutineScope(controller.context + dataPageJobs + handler)

    @Volatile
    var bitmap: FlexibleBitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: PageState = PageState.STUB

    @Volatile
    var pageInfo: PageInfo? = null

    internal val page = document.getOrCreatePageAdapter(pageNum)

    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val layoutInfo: LayoutPosition = LayoutPosition(pageNumber = pageNum)

    @Volatile
    lateinit var pageInfoJob: Job
        private set

    @Volatile
    private var rawSize: Deferred<PageSize>? = null

    @Volatile
    private var pageData: Deferred<Unit>? = null

    fun init() {
       reinit("init")
    }

    fun readPageDataFromUI(): Deferred<Unit> {
        if (pageData == null) {
            pageData = dataPageScope.async { page.readPageDataForRendering() }
        }
        return pageData!!
    }

    fun readRawSizeFromUI(): Deferred<PageSize> {
        if (rawSize == null) {
            rawSize = dataPageScope.async { page.getPageSize() }
        }
        return rawSize!!
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

    internal fun cancelChildJobs(allJobs: Boolean = false) {
        renderingPageJobs.cancelChildren()
        if (allJobs) {
            dataPageJobs.cancelChildren()
        }
    }

    private suspend fun waitJobsCancellation(allJobs: Boolean = false) {
        renderingPageJobs.cancelAndJoin()
        if (allJobs) {
            dataPageJobs.cancelAndJoin()
        }
    }

    private fun freePagePointer() {
        page.destroy()
    }

    fun reinit(marker: String = "reinit") {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        log("Page $pageNum $marker $state $document" )
        cancelChildJobs()
        if (::pageInfoJob.isInitialized) {
            pageInfoJob.cancel()
        }
        pageInfo = null
        pageInfoJob = dataPageScope.async {
            val info = getPageInfo(controller.layoutStrategy as SimpleLayoutStrategy)
            withContext(Dispatchers.Main) {
                controller.layoutStrategy.reset(layoutInfo, info)
                initBitmap(layoutInfo, info)
            }
        }
    }

    private fun initBitmap(layoutInfo: LayoutPosition, info: PageInfo) {
        if (state == PageState.SIZE_AND_BITMAP_CREATED) return
        val oldSize = Rect(wholePageRect)
        wholePageRect.set(0, 0, layoutInfo.x.pageDimension, layoutInfo.y.pageDimension)
        bitmap = bitmap?.resize(wholePageRect.width(), wholePageRect.height(), controller.bitmapCache)
            ?: pageLayoutManager.bitmapManager.createDefaultBitmap(wholePageRect.width(), wholePageRect.height(), pageNum)
        log("PageView.initBitmap $pageNum ${controller.document} $wholePageRect")
        pageInfo = info
        state = PageState.SIZE_AND_BITMAP_CREATED
        pageLayoutManager.onPageSizeCalculated(this, oldSize, info)
    }

    fun draw(canvas: Canvas, scene: OrionDrawScene) {
        if (state != PageState.STUB && bitmap != null) {
            //draw bitmap
            log("Draw page $pageNum in state $state ${bitmap?.width} ${bitmap?.height} ")
            draw(canvas, bitmap!!, scene.defaultPaint!!, scene)
        } else {
            log("Draw border $pageNum in state $state")
            drawBorder(canvas, scene)
        }
    }

    internal fun precacheData() {
        readRawSizeFromUI()
        readPageDataFromUI()
    }

    fun visibleRect(): Rect? {
        return layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)
    }

    internal fun renderVisible() {
        if (!isOnScreen) {
            log("Non visible $pageNum");
            return
        }

        renderingScope.launch {
            layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)?.let {
                render(it, true, "Render visible")
            }
        }
    }

    fun launchJobInRenderingScope(body: suspend () -> Unit): Job {
        return renderingScope.launch {
            body()
        }
    }

    fun renderInvisible(rect: Rect, tag: String) {
        //TODO yield
        if (Rect.intersects(rect, wholePageRect)) {
            renderingScope.launch {
                render(rect, false, "Render invisible $tag")
            }
        }
    }

    suspend fun <T> renderForCrop(body: suspend () -> T): T {
        return withContext(controller.renderingDispatcher) {
            body()
        }
    }

    private suspend fun render(rect: Rect, fromUI: Boolean, tag: String) {
        readPageDataFromUI().await()

        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return

        if (state != PageState.SIZE_AND_BITMAP_CREATED) {
            return
        }
        //val bound = tempRegion.bounds
        val bound = Rect(rect)
        val bitmap = bitmap!!

        renderingScope.launch {
            timing("$tag $pageNum page in rendering engine: $bound") {
                bitmap.render(bound, layoutInfo, page)
            }
            if (isActive) {
                withContext(Dispatchers.Main) {
                    if (kotlin.coroutines.coroutineContext.isActive) {
                        if (fromUI) {
                            log("PageView ($tag) invalidate: $pageNum $layoutData ${scene != null}")
                            if (this@PageView.isOnScreen) {//TODO active
                                scene?.invalidate()
                            }
                        }
                    }
                }
            } else {
                log("PageView.render $pageNum $layoutData: canceled")
            }
        }/*.join()*/

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
        cancelChildJobs()
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