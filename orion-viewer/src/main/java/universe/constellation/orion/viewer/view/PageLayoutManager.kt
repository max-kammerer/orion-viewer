package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.math.MathUtils
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.bitmap.BitmapManager
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.calcPageLayout
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.selection.PageAndSelection
import kotlin.math.abs
import kotlin.math.max

private const val VISIBLE_PAGE_LIMIT = 5

class PageLayoutManager(val controller: Controller, val scene: OrionDrawScene) {

    class Callback(val page: Int, val job: CompletableJob, val body: (PageInfo) -> Unit)

    private val analytics = controller.activity.analytics

    private val handler = CoroutineExceptionHandler { _, ex ->
        errorInDebug("Processing error in PageLayoutManager", ex)
        analytics.error(ex)
    }

    val bitmapManager: BitmapManager = BitmapManager(this)

    val activePages: MutableList<PageView> = arrayListOf()

    val visiblePages: List<PageView>
        get() = activePages.filter { it.isActivePage }

    private val PageView.isActivePage: Boolean
        get() = isOnScreen && (!isSinglePageMode || pageNum == activePage)

    private val PageView.isActiveOrOnScreen: Boolean
        get() = isOnScreen || pageNum == activePage

    private var onPageSizeCalculatedCallback: Callback? = null

    var isSinglePageMode = false

    private var activePage = -1

    val sceneRect = Rect(0, 0, scene.width, scene.height)

    private val tmpRect = Rect()
    private val tmpRectF = RectF()
    private val tmpRectF2 = RectF()


    private var sceneWidth: Int
        get() = sceneRect.width()
        set(value) {
            sceneRect.right = value
        }

    private var sceneHeight: Int
        get() = sceneRect.height()
        set(value)  {
            sceneRect.bottom = value
        }

    fun onDimensionChanged(newWidth: Int, newHeight: Int) {
        if (sceneWidth != newWidth || newHeight != sceneHeight) {
            log("New scene size: $newWidth $newHeight, old=$sceneRect")
            sceneWidth = newWidth
            sceneHeight = newHeight
            updateRenderingParameters()
        }
    }

    private fun updateRenderingParameters() {
        //zoom
        isSinglePageMode = false
        updateCacheAndRender()
    }

    fun forcePageUpdate() {
        activePages.forEach {
            it.invalidateAndUpdate()
            log("Page force update " + it.pageNum)
        }
    }

    fun destroy() {
        activePages.forEach {
            it.destroy()
        }
        activePages.clear()
    }

    fun performTouchZoom(zoom: Float, startFocus: PointF, endFocus: PointF) {
        activePages.forEach {
            it.invalidateAndMoveToStub()
        }

        val tmp = PointF()
        activePages.forEach {
            tmp.set(it.layoutData.position)
            tmp.minusOffset(startFocus)
            tmp.zoom(zoom)
            tmp.offset(startFocus)
            it.layoutData.position.set(tmp)
            it.wholePageRect.zoom(zoom)
        }

        //TODO delete pages
        activePages.forEach {
            it.reinit()
        }
    }

    fun currentPageLayout(): LayoutPosition? {
        val page = if (isSinglePageMode) {
            activePages.firstOrNull { it.pageNum == activePage && it.isOnScreen }
                //?: errorInDebugOr("No page in isSinglePageMode") { null }
        } else {
            activePages.firstOrNull { it.isOnScreen } //?: errorInDebugOr("No page in general mode") { null }
        }

        val layoutInfo = page?.layoutInfo ?: return null
        val position = page.layoutData.position
        layoutInfo.x.offset = -position.x.toInt()
        layoutInfo.y.offset = -position.y.toInt()
        return layoutInfo
    }

    private fun setSinglePageMode(enable: Boolean, page: Int) {
        isSinglePageMode = enable
        activePage = if (isSinglePageMode) page else -1
    }

    fun doScroll(xPos: Float, yPos: Float, distanceX: Float, distanceY: Float) {
        setSinglePageMode(false, -1)
        doScrollAndDoRendering(xPos, yPos, distanceX, distanceY)
        scene.postInvalidate()
    }

    private fun doScrollAndDoRendering(
        xPos: Float,
        yPos: Float,
        distanceX: Float,
        distanceY: Float,
        isTapNavigation: Boolean = false
    ) {
        val distanceY2 = if (isTapNavigation) distanceY else clampLimits(distanceY)
        if (distanceY2 == 0f && distanceX == 0f) return

        activePages.forEach {
            val layoutData = it.layoutData
            if (layoutData.containsY(yPos)) {
                val leftDelta = layoutData.globalLeft - sceneRect.left
                val righDelta = sceneRect.right - layoutData.globalRight
                if (layoutData.wholePageRect.width() < sceneRect.width()) {
                    if (abs(leftDelta - righDelta) >= abs(leftDelta - righDelta + 2 * distanceX)) {
                        layoutData.position.x += distanceX
                    }
                } else if (!isTapNavigation) {
                    var newDistance = 0f
                    if (distanceX > 0 && layoutData.globalLeft < 0) {
                        newDistance = MathUtils.clamp(distanceX, distanceX, sceneRect.left - layoutData.globalLeft,)
                    } else if (distanceX < 0 && layoutData.globalRight > sceneRect.right) {
                        newDistance = -MathUtils.clamp(-distanceX, -distanceX, layoutData.globalRight - sceneRect.right)
                    }
                    layoutData.position.x += newDistance
                } else {
                    layoutData.position.x += distanceX
                }
            }

            doYScrollAndUpdateState(distanceY2, it)
        }
        updateCacheAndRender()
        dump()
    }

    private fun doYScrollAndUpdateState(
        distanceY: Float,
        it: PageView
    ) {
        it.layoutData.position.y += distanceY
        it.updateState()
    }

    private val toDestroy = hashSetOf<PageView>()

    fun updateCacheAndRender() {
        updateCache()

        //stop rendering
        for (page in activePages) {
            page.cancelChildJobs()
        }

        //First of all: render single active page and precache data around
        if (isSinglePageMode) {
            val mainPage = activePages.firstOrNull { it.isActivePage }
            println("Render... ${mainPage?.pageNum}")
            //if (mainPage?.state != PageState.SIZE_AND_BITMAP_CREATED) return
            mainPage?.renderVisible()
            mainPage?.precache()
        }

        //Second of all: render active or hidden on screen pages
        for (page in activePages) {
            if (isSinglePageMode && page.isActivePage) continue
            page.renderVisible()
        }

        //Third do precaching for all
        for (page in activePages) {
            if (isSinglePageMode && page.isActivePage) continue
            page.precache()
        }

        activePages.firstOrNull()?.takeIf { it.isOnScreen }?.precacheNeighbours(false)

        activePages.lastOrNull()?.takeIf { it.isOnScreen }?.precacheNeighbours(true)
    }

    private fun updateCache() {
        var head = true
        activePages.zipWithNext { f, s ->
            val oldState = f.state
            if (head && !s.isActiveOrOnScreen && !f.isActiveOrOnScreen) {
                toDestroy.add(f)
                f.destroy()
                log("updateCache: ${f.pageNum} destroyed")
            } else {
                head = false
            }
            if (f.state != oldState) {
                log("updateCache: ${f.pageNum} newState=${f.state} oldState=$oldState")
            }
        }

        head = true
        activePages.asReversed().zipWithNext { f, s ->
            val oldState = f.state
            if (head && !s.isActiveOrOnScreen && !f.isActiveOrOnScreen) {
                if (toDestroy.add(f)) {
                    f.destroy()
                    log("updateCache: ${f.pageNum} destroyed")
                }
            } else {
                head = false
            }
            if (f.state != oldState) {
                log("updateCache: ${f.pageNum} newState=${f.state} oldState=$oldState")
            }
        }
        activePages.removeAll(toDestroy)
        toDestroy.clear()
    }

    private fun clampLimits(distanceY: Float): Float {
        if (activePages.isNotEmpty()) {
            if (distanceY > 0) {
                val first = activePages.first()
                if (first.pageNum == 0) {
                    val pageYPos = first.layoutData.position.y
                    return MathUtils.clamp(distanceY, distanceY, max(0f, -pageYPos))
                }
            } else if (distanceY < 0) {
                val last = activePages.last()
                if (last.pageNum == controller.pageCount - 1) {
                    val bottomPage = last.layoutData.globalBottom
                    return -MathUtils.clamp(-distanceY, -distanceY, max(0f, bottomPage - sceneRect.bottom))
                }
            }
        }
        return distanceY
    }

    internal fun uploadPrevPage(page: PageView, addIfAbsent: Boolean = false): PageView? {
        if (page.pageNum <= 0) return null
        val prevPageNum = page.pageNum - 1
        val existingPage = activePages.firstOrNull { it.pageNum == prevPageNum }
        if (existingPage != null) {
            //updateStateAndRenderVisible(existingPage)
            return existingPage
        }

        val pageStart = page.layoutData.position.y
        if (pageStart > 5 || addIfAbsent) {
            val newView = controller.createPageView(prevPageNum)
            addPageInPosition(
                newView,
                0f,
                page.layoutData.position.y - newView.layoutData.wholePageRect.height() - 2,
                0
            )
            return newView
        }
        return null
    }

    internal fun uploadNextPage(page: PageView, addIfAbsent: Boolean = false): PageView? {
        val nextPageNum = page.pageNum + 1
        if (nextPageNum >= controller.pageCount) return null
        val existingPage = activePages.firstOrNull { it.pageNum == nextPageNum }
        if (existingPage != null) {
            return existingPage
        }

        val pageEnd = page.pageEndY + 2
        if (pageEnd < sceneHeight || addIfAbsent) {
            return addPageInPosition(nextPageNum, pageEnd)
        }
        return null
    }

    private fun addPageInPosition(pageNum: Int, pageYPos: Float): PageView {
        val view = controller.createPageView(pageNum)
        addPageInPosition(view, pageYPos = pageYPos)
        return view
    }

    private fun addPageInPosition(view: PageView, pageXPos: Float = 0f, pageYPos: Float, index: Int = activePages.size) {
        view.layoutData.position.y = pageYPos
        view.layoutData.position.x = pageXPos
        activePages.add(index, view)
        view.scene = scene
        view.updateState()
    }

    private fun PageView.updateState() {
        bitmapManager.actualizeActive(this)
    }

    fun findPageAndPageRect(screenRect: Rect): List<PageAndSelection> {
        return activePages.mapNotNull {
            val pageSelection = it.layoutData.pagePartOnScreen(screenRect, Rect()) ?: return@mapNotNull null
            log("selection: " + it.pageNum + pageSelection)
            //TODO zoom and crop
            pageSelection.minusOffset(it.layoutData.position)
            val layoutInfo = it.layoutInfo
            val zoom  = layoutInfo.docZoom
            pageSelection.offset(layoutInfo.x.marginLeft, layoutInfo.y.marginLeft)
            pageSelection.set((pageSelection.left/zoom).toInt(),
                (pageSelection.top/zoom).toInt(),
                (pageSelection.right/zoom).toInt(), (pageSelection.bottom/zoom).toInt()
            )
            log("selection: " + it.pageNum + pageSelection)
            PageAndSelection(it.page, pageSelection)
        }
    }

    fun renderVisiblePages(canvas: Canvas, scene: OrionDrawScene) {
        if (isSinglePageMode) {
            val active = activePages.firstOrNull { it.pageNum == activePage }
            if (active != null) {
                renderPage(active, canvas, scene, true)
                return
            }
            errorInDebug("No active page $activePage, $active")
        }

        var first = true
        activePages.forEach {
            if (it.isOnScreen) {
                renderPage(it, canvas, scene, first)
                first = false
            }
        }
    }

    private fun renderPage(
        it: PageView,
        canvas: Canvas,
        scene: OrionDrawScene,
        isFirst: Boolean
    ) {
        it.draw(canvas, scene)
        if (isFirst) {
            val visibleRect = it.visibleRect()
            scene.orionStatusBarHelper.onPageUpdate(
                it.pageNum,
                visibleRect?.left ?: 0,
                visibleRect?.top ?: 0
            )
        }
    }

    fun renderNextOrPrev(next: Boolean, isTapNavigation: Boolean = false): Pair<PageView, Job>? {
        currentPageLayout()?.let {
            val copy = it.copy()
            val layoutStrategy = controller.layoutStrategy
            val currentPageNum = copy.pageNumber
            log("renderNextOrPrev $copy")
            when (val res = layoutStrategy.calcPageLayout(copy, next)) {
                0 -> {
                    log("renderNextOrPrev new params: $copy")
                    if (isTapNavigation && !next && copy.y.offset < 0) { copy.y.offset = 0 }
                    return controller.drawPage(copy, isTapNavigation = isTapNavigation)
                }

                1 ->
                    if (currentPageNum + 1 < controller.document.pageCount) {
                        return renderPageAt(currentPageNum + 1, 0, 0, isTapNavigation) { page, info ->
                            val pos = page.layoutInfo.copy()
                            layoutStrategy.reset(pos, info, next)
                            val oldPosition = page.layoutData.position
                            val x = -pos.x.offset
                            //val y = getCenteredYInSinglePageMode(-pos.y.offset, page.height)
                            val y = -pos.y.offset

                            doScrollAndDoRendering(
                                oldPosition.x,
                                oldPosition.y,
                                x - oldPosition.x,
                                y - oldPosition.y,
                                isTapNavigation
                            )
                        }
                    }

                -1 ->
                    if (currentPageNum > 0) {
                        return renderPageAt(currentPageNum - 1, 0, 0, isTapNavigation) { page, info ->
                            val pos = page.layoutInfo.copy()
                            layoutStrategy.reset(pos, info, next)
                            val oldPosition = page.layoutData.position
                            val x = -pos.x.offset
                            //val y = getCenteredYInSinglePageMode(-pos.y.offset, page.height)
                            val y = -pos.y.offset
                            doScrollAndDoRendering(
                                oldPosition.x,
                                oldPosition.y,
                                x - oldPosition.x,
                                y - oldPosition.y,
                                isTapNavigation
                            )
                        }
                    }

                else -> throw RuntimeException("Unknown result $res")
            }
        }
        return null
    }

    private fun getCenteredYInSinglePageMode(y: Int, pageHeight: Int): Float {
        if (isSinglePageMode) {
            if (pageHeight < sceneHeight) {
                return (sceneHeight - pageHeight) / 2.0f
            }
        }
        return y.toFloat()
    }

    fun renderPageAt(
        pageNum: Int,
        x: Int,
        y: Int,
        isTapNavigation: Boolean = false,
        doScroll: (PageView, PageInfo) -> Unit = { page, _ ->
            val newPosition = page.layoutData.position
            doScrollAndDoRendering(
                newPosition.x,
                newPosition.y,
                x - newPosition.x,
                y - newPosition.y,
                isTapNavigation
            )
        }
    ): Pair<PageView, Job> {
        log("RenderPageAt $pageNum $x $y $isTapNavigation")
        setSinglePageMode(isTapNavigation, pageNum)

        val index = activePages.binarySearch { it.pageNum.compareTo(pageNum) }

        val page = if (index >= 0) {
            activePages[index]
        } else {
            val insertIndex = -(index + 1)

            val destroy = when (insertIndex) {
                0 -> (activePages.firstOrNull()?.pageNum ?: -1) != pageNum + 1
                activePages.size -> (activePages.lastOrNull()?.pageNum ?: -1) != pageNum - 1
                else -> false
            }
            println("insertIndex $pageNum: $insertIndex ${activePages.size} $destroy")
            if (destroy) {
                destroyPages()
            }
            val newPage = if (activePages.isEmpty()) {
                val newPage = controller.createPageView(pageNum)
                addPageInPosition(newPage, -x.toFloat(), -y.toFloat(), if (destroy) 0 else insertIndex)
                newPage
            } else {
                val page = if (insertIndex == 0) {
                    uploadPrevPage(activePages.first(), addIfAbsent = true)
                } else {
                    uploadNextPage(activePages.last(), addIfAbsent = true)
                }
                val toMove = 0 - page!!.layoutData.position.y
                activePages.forEach {
                    doYScrollAndUpdateState(toMove, it)
                }
                page
            }
            newPage
        }

        onPageSizeCalculatedCallback?.job?.cancel()
        if (page.state == PageState.SIZE_AND_BITMAP_CREATED) {
            println("onPageSizeCalculatedCallback: inplace")
            onPageSizeCalculatedCallback = null
            val completed = page.pageInfo!!
            doScroll(page, completed) //TODO process errors
            return page to page.launchJobInRenderingScope {
                page.renderVisible()
            }
        } else {
            if (page.state == PageState.SIZE_AND_BITMAP_CREATED) {
                dump()
                errorInDebug("Unexpected state ${page.pageNum} + ${page.pageInfoJob.isCompleted}")
            }
            println("onPageSizeCalculatedCallback: lazy")
            onPageSizeCalculatedCallback = Callback(pageNum, Job(controller.rootJob)) {
                println("onPageSizeCalculatedCallback: call")
                doScroll(page, it)
            }
            return page to onPageSizeCalculatedCallback!!.job
        }
    }

    private fun destroyPages() {
        log("Descroying pages")
        val iterator = activePages.iterator()
        for (page in iterator) {
            //TODO optimize
            page.destroy()
            iterator.remove()
        }
    }

    private fun dump() {
        activePages.forEach{
            log("Dump ${it.pageNum} ${it.state}: ${it.layoutData.globalRect(tmpRectF)}")
        }

        if (!BuildConfig.DEBUG) return

        activePages.zipWithNext().forEach {
            val rect1 = it.first.layoutData.globalRect(tmpRectF)
            val rect2 = it.second.layoutData.globalRect(tmpRectF2)
            if (RectF.intersects(rect1, rect2)) {
                val intersection = RectF(rect1)
                intersection.intersect(rect2)
                val message =
                    "intersected pages ${it.first.pageNum} and ${it.second.pageNum} rects: $rect1 $rect2, intersection $intersection"
                errorInDebug(message)
            }
        }
    }

    fun isVisible(view: PageView): Boolean {
        return view.layoutData.pagePartOnScreen(sceneRect, tmpRect) != null
    }

    fun onPageSizeCalculated(updatedView: PageView, oldArea: Rect, info: PageInfo) {
        log("onSizeCalculated ${updatedView.pageNum}: ${updatedView.layoutData} old=$oldArea")
        val delta = updatedView.wholePageRect.height() - oldArea.height()

        var found = false
        val position = updatedView.layoutData.position
        if (position.y < 0) {
            for (page in activePages) {
                page.layoutData.position.y -= delta
                page.updateState()
                if (updatedView === page) break
            }
        } else {
            for (page in activePages) {
                if (found) {
                    page.layoutData.position.y += delta
                    page.updateState()
                } else if (page === updatedView) {
                    found = true
                }
            }
        }

        val pageWidth = updatedView.wholePageRect.width()
        val sceneWidth = sceneRect.width()
        if (pageWidth < sceneWidth) {
            position.x = (sceneWidth - pageWidth) / 2.0f
        }

        val callback = onPageSizeCalculatedCallback
        val job = if (callback?.page == updatedView.pageNum) {
            onPageSizeCalculatedCallback = null
            callback.body(info)
            callback.job
        } else {
            null
        }

        updatedView.updateState()
        updateCacheAndRender()

        if (updatedView.isActivePage) {
            scene.invalidate()
        }
        dump()
    }

    fun onViewSizeChanged() {
        //TODO find old anchor and use it for relayouting
    }

    fun defaultSize(): Rect {
        val rect = Rect(0, 0, scene.width, scene.height)
        if (rect.width() == 0) {
            rect.right = 400
        }

        if (rect.height() == 0) {
            rect.bottom = 400
        }
        return rect
    }

    fun serialize(info: LastPageInfo) {
        info.isSinglePageMode = isSinglePageMode
    }
}

fun Rect.offset(p: PointF) {
    this.offset(p.x.toInt(), p.y.toInt())
}

fun Rect.minusOffset(p: PointF) {
    this.offset(-p.x.toInt(), -p.y.toInt())
}


fun PointF.offset(p: PointF) {
    this.offset(p.x, p.y)
}

fun PointF.zoom(zoom: Float) {
    this.set(x * zoom, y * zoom)
}

fun RectF.zoom(zoom: Float) {
    this.set(left * zoom, right * zoom, top * zoom, bottom * zoom)
}

fun Rect.zoom(zoom: Float) {
    this.set((left * zoom).toInt(), (top * zoom).toInt(), (right * zoom).toInt(), (bottom * zoom).toInt())
}

fun PointF.minusOffset(p: PointF) {
    this.offset(-p.x, -p.y)
}