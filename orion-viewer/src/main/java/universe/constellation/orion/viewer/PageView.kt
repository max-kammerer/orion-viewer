package universe.constellation.orion.viewer

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
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.PageLayoutManager
import universe.constellation.orion.viewer.view.precache

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

    private val handler = CoroutineExceptionHandler { _, ex ->
        log("Processing error for page $pageNum")
        ex.printStackTrace()
        //TODO processing
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

    private var pageJobs = SupervisorJob(rootJob)

    @Volatile
    var bitmap: FlexibleBitmap? = null

    //TODO reset on lp changes
    @Volatile
    var state: PageState = PageState.STUB

    internal val page = document.getOrCreatePageAdapter(pageNum)

    init {
        wholePageRect.set(pageLayoutManager.defaultSize())
    }

    val layoutInfo: LayoutPosition = LayoutPosition()

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
        log("Destroy $pageNum")
        toInvisible()
        state = PageState.DESTROYED
        pageJobs.cancel()
        bitmap?.disableAll(controller.bitmapCache)
        bitmap = null
        GlobalScope.launch(controller.context) {
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
            val info = page.getPageInfo(controller.layoutStrategy as SimpleLayoutStrategy, controller.layoutStrategy.margins.cropMode)
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

    fun visibleRect(): Rect? {
        return layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)
    }

    internal suspend fun renderVisible(): Deferred<PageView?>? {
        if (!isOnScreen) {
            log("Non visible $pageNum");
            return null
        }

        return coroutineScope {
            async (Dispatchers.Main + handler) {
                layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)?.let {
                    render(it, true)?.await()
                }
            }
        }
    }

    internal suspend fun renderInvisible(rect: Rect): Deferred<PageView?>? {
        //TODO yield
        if (Rect.intersects(rect, wholePageRect)) {
            return render(rect, false)
        }
        return null
    }

    internal suspend fun render(rect: Rect, fromUI: Boolean): Deferred<PageView>? {
        val layoutStrategy = controller.layoutStrategy
        if (!(layoutStrategy.viewWidth > 0 &&  layoutStrategy.viewHeight > 0)) return null

        if (state != PageState.SIZE_AND_BITMAP_CREATED) {
            pageInfo.await()
        }
        //val bound = tempRegion.bounds
        val bound = Rect(rect)
        val bitmap = bitmap!!

        return coroutineScope {
            async(controller.context + pageJobs + handler) {
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
                    log("PageView.render: canceled")
                }
                this@PageView
            }
        }

    }

    private fun draw(canvas: Canvas, bitmap: FlexibleBitmap, defaultPaint: Paint, scene: OrionDrawScene) {
        canvas.save()
        canvas.translate(layoutData.position.x, layoutData.position.y)
        bitmap.draw(canvas, layoutData.wholePageRect, defaultPaint)
        drawBorder(canvas, scene)
        canvas.restore()
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