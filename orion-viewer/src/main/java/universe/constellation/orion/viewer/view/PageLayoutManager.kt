package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.math.MathUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.bitmap.BitmapManager
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.calcPageLayout
import universe.constellation.orion.viewer.layout.reset
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.selection.PageAndSelection
import kotlin.math.abs
import kotlin.math.max

private const val VISIBLE_PAGE_LIMIT = 5

class PageLayoutManager(val controller: Controller, val scene: OrionDrawScene) {

    private val analytics = controller.activity.analytics

    private val handler = CoroutineExceptionHandler { _, ex ->
        errorInDebug("Processing error in PageLayoutManager", ex)
        analytics.error(ex)
    }

    val bitmapManager: BitmapManager = BitmapManager(this)

    val activePages: MutableList<PageView> = arrayListOf()

    val visiblePages: List<PageView>
        get() = activePages.filter { it.isOnScreen && it.isVisibleState }

    var isSinglePageMode = false

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
        uploadNewPages()
    }

    fun forcePageUpdate() {
        activePages.forEach {
            it.invalidateAndUpdate()
            log("Page force update " + it.pageNum)
        }
    }

    fun destroy() {
        activePages.forEach {
            it.toInvisible()
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
        //TODO
        val page = activePages.firstOrNull { it.isOnScreen } ?: return null
        val layoutInfo = page.layoutInfo
        val position = page.layoutData.position
        layoutInfo.x.offset = -position.x.toInt()
        layoutInfo.y.offset = -position.y.toInt()
        return layoutInfo
    }

    fun doScroll(xPos: Float, yPos: Float, distanceX: Float, distanceY: Float) {
        isSinglePageMode = false
        doScrollOnly(xPos, yPos, distanceX, distanceY)
        uploadNewPages()
        scene.postInvalidate()
    }

    private fun doScrollOnly(xPos: Float, yPos: Float, distanceX: Float, distanceY: Float, isTapNavigation: Boolean = false) {
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
        updateCache()
        dump()
    }

    private fun doYScrollAndUpdateState(
        distanceY: Float,
        it: PageView
    ) {
        it.layoutData.position.y += distanceY
        updateStateAndRenderVisible(it)
    }

    private val toDestroy = hashSetOf<PageView>()

    private fun updateCache() {
        var head = true
        activePages.zipWithNext { f, s ->
            val oldState = f.state
            if (head && !s.isVisibleState && !f.isVisibleState) {
                toDestroy.add(f)
                f.destroy()
                log("Destroyed ${f.pageNum}")
            } else {
                head = false
            }
            log("Cache update: ${f.pageNum} newState=${f.state} oldState=$oldState")
        }

        head = true
        activePages.asReversed().zipWithNext { f, s ->
            val oldState = f.state
            if (head && !s.isVisibleState && !f.isVisibleState) {
                if (toDestroy.add(f)) {
                    f.destroy()
                    log("Destroyed ${f.pageNum}")
                }
            } else {
                head = false
            }
            log("Cache update: ${f.pageNum} newState=${f.state} oldState=$oldState")
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

    fun uploadNewPages() {        //zoom
        if (isSinglePageMode) return
        if (activePages.size >= VISIBLE_PAGE_LIMIT) {
            if (activePages.size == VISIBLE_PAGE_LIMIT && activePages.first().layoutData.position.y < 0) {
                //upload additional page
            } else {
                //don't add new pages
                return
            }
        }
        log("Before uploadNewPages")
        dump()
        if (activePages.isEmpty()) {
            val nextPageNum = 0
            val view = controller.createPageView(nextPageNum)
            addPageInPosition(view, pageYPos = 0f)
        } else {
            uploadNextPage(activePages.last())

            uploadPrevPage(activePages.first())
        }
        log("After uploadNewPages")
        dump()
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
        updateStateAndRenderVisible(view)
    }

    private fun updateStateAndRenderVisible(view: PageView) {
        if (view.updateState()) {
            view.renderVisibleAsync()
        }
    }

    private fun PageView.updateState(): Boolean {
        bitmapManager.actualizeActive(this)
        val visible = this.isOnScreen
        if (!visible) {
            log("toInvisible ${this.pageNum}: ${this.layoutData.globalRect(tmpRect)} $sceneRect")
            this.toInvisible()
        } else {
            this.toVisible()
        }
        return visible
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
        var first = true
        activePages.forEach {
            if (it.isOnScreen) {
                it.draw(canvas, scene)
                if (first) {
                    val visibleRect = it.visibleRect()
                    scene.orionStatusBarHelper.onPageUpdate(it.pageNum, visibleRect?.left ?: 0, visibleRect?.top ?: 0)
                    first = false
                }
                if (isSinglePageMode) return
            }
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
                        return renderPageAt(currentPageNum + 1, 0, 0, isTapNavigation) { page ->
                            val pos = page.layoutInfo.copy()
                            layoutStrategy.reset(pos, page.page, next)
                            val oldPosition = page.layoutData.position
                            val x = -pos.x.offset
                            val y = -pos.y.offset
                            doScrollOnly(
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
                        return renderPageAt(currentPageNum - 1, 0, 0, isTapNavigation) { page ->
                            val pos = page.layoutInfo.copy()
                            layoutStrategy.reset(pos, page.page, next)
                            val oldPosition = page.layoutData.position
                            val x = -pos.x.offset
                            val y = -pos.y.offset
                            doScrollOnly(oldPosition.x, oldPosition.y, x - oldPosition.x, y - oldPosition.y, isTapNavigation)
                        }
                    }

                else -> throw RuntimeException("Unknown result $res")
            }
        }
        return null
    }

    fun renderPageAt(pageNum: Int, x: Int, y: Int, isTapNavigation: Boolean = false, doScroll: (PageView) -> Unit = {page ->
        val newPosition = page.layoutData.position
        doScrollOnly(newPosition.x, newPosition.y, x - newPosition.x, y - newPosition.y, isTapNavigation)
    }): Pair<PageView, Job> {
        println("RenderPageAt $pageNum $x $y")
        isSinglePageMode = isTapNavigation
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

        return page to GlobalScope.launch(Dispatchers.Main + page.pageJobs +  handler) {
            page.pageInfo.await()
            //if (page.state != PageState.DESTROYED) {
                //if (isVisible(page)) {
                    doScroll(page)
                    page.updateState()
                    page.renderVisible()
                //}
            //}
        }
    }

    private fun destroyPages() {
        val iterator = activePages.iterator()
        for (page in iterator) {
            //TODO optimize
            page.toInvisible()
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

    fun onPageSizeCalculated(updatedView: PageView, oldArea: Rect) {
        log("onSizeCalculated ${updatedView.pageNum}: ${updatedView.layoutData} old=$oldArea")
        val delta = updatedView.wholePageRect.height() - oldArea.height()

        var found = false
        if (updatedView.layoutData.position.y < 0) {
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
            updatedView.layoutData.position.x = (sceneWidth - pageWidth) / 2.0f
        }
        updateStateAndRenderVisible(updatedView)
        updateCache()

        scene.invalidate()
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