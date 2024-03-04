package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.math.MathUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.PageState
import universe.constellation.orion.viewer.PageView
import universe.constellation.orion.viewer.bitmap.BitmapManager
import universe.constellation.orion.viewer.handler
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.calcPageLayout
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.selection.PageAndSelection
import kotlin.math.abs
import kotlin.math.max

private const val VISIBLE_PAGE_LIMIT = 5

class PageLayoutManager(val controller: Controller, val scene: OrionDrawScene): ViewDimensionAware {

    init {
        scene.setDimensionAware(this)
    }

    val bitmapManager: BitmapManager = BitmapManager(this)

    val visiblePages: MutableList<PageView> = arrayListOf()

    var isSinglePageMode = false

    val sceneRect = Rect(0, 0, scene.width, scene.height)

    private val tmpRect = Rect(sceneRect)

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

    override fun onDimensionChanged(newWidth: Int, newHeight: Int) {
        if (sceneWidth != newWidth && newHeight != sceneHeight) {
            log("New scene size: $sceneRect")
            sceneWidth = newWidth
            sceneHeight = newHeight
            updateRenderingParameters()
        }
    }

    fun updateRenderingParameters() {
        //zoom
        isSinglePageMode = false
        uploadNewPages()
    }

    fun forcePageUpdate() {
        visiblePages.forEach {
            it.invalidateAndUpdate()
            log("Page force update " + it.pageNum)
        }
    }

    fun destroy() {
        visiblePages.forEach {
            it.toInvisible()
            it.destroy()
        }
        visiblePages.clear()
    }

    fun performTouchZoom(zoom: Float, startFocus: PointF, endFocus: PointF) {
        visiblePages.forEach {
            it.invalidateAndMoveToStub()
        }

        val tmp = PointF()
        visiblePages.forEach {
            tmp.set(it.layoutData.position)
            tmp.minusOffset(startFocus)
            tmp.zoom(zoom)
            tmp.offset(startFocus)
            it.layoutData.position.set(tmp)
            it.wholePageRect.zoom(zoom)
        }

        //TODO delete pages
        visiblePages.forEach {
            it.reinit()
        }
    }

    fun currentPageLayout(): LayoutPosition? {
        //TODO
        val page = visiblePages.firstOrNull() ?: return null
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

    private fun doScrollOnly(xPos: Float, yPos: Float, distanceX: Float, distanceY: Float) {
        val distanceY2 = clampLimits(distanceY)
        if (distanceY2 == 0f && distanceX == 0f) return

        visiblePages.forEach {
            val layoutData = it.layoutData
            layoutData.position.y += distanceY2
            if (layoutData.contains(xPos, yPos)) {
                val leftDelta = layoutData.globalLeft - sceneRect.left
                val righDelta = sceneRect.right - layoutData.globalRight
                if (layoutData.wholePageRect.width() < sceneRect.width()) {
                    if (abs(leftDelta - righDelta) >= abs(leftDelta - righDelta + 2 * distanceX)) {
                        layoutData.position.x += distanceX
                    }
                } else {
                    var newDistance = 0f
                    if (distanceX > 0 && layoutData.globalLeft < 0) {
                        newDistance = MathUtils.clamp(distanceX, distanceX, sceneRect.left - layoutData.globalLeft,)
                    } else if (distanceX < 0 && layoutData.globalRight > sceneRect.right) {
                        newDistance = -MathUtils.clamp(-distanceX, -distanceX, layoutData.globalRight - sceneRect.right)
                    }
                    layoutData.position.x += newDistance
                }
            }
            updateStateAndRenderVisible(it)
        }
        updateCache()
        dump()
    }

    private val toDestroy = hashSetOf<PageView>()

    private fun updateCache() {
        visiblePages.zipWithNext { f, s ->
            if (s.state == PageState.CAN_BE_DELETED && f.state == PageState.CAN_BE_DELETED) {
                toDestroy.add(f)
                f.destroy()
                log("Destroyed ${f.pageNum}")
            }
            println("${f.pageNum} ${f.state}")
        }
        visiblePages.removeAll(toDestroy)
        toDestroy.clear()
    }

    private fun clampLimits(distanceY: Float): Float {
        if (visiblePages.isNotEmpty()) {
            if (distanceY > 0) {
                val first = visiblePages.first()
                if (first.pageNum == 0) {
                    val pageYPos = first.layoutData.position.y
                    return MathUtils.clamp(distanceY, distanceY, max(0f, -pageYPos))
                }
            } else if (distanceY < 0) {
                val last = visiblePages.last()
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
        if (visiblePages.size >= VISIBLE_PAGE_LIMIT) {
            if (visiblePages.size == VISIBLE_PAGE_LIMIT && visiblePages.first().layoutData.position.y < 0) {
                //upload additional page
            } else {
                //don't add new pages
                return
            }
        }
        log("Before uploadNewPages")
        dump()
        if (visiblePages.isEmpty()) {
            val nextPageNum = 0
            val view = controller.createCachePageView(nextPageNum)
            addPageInPosition(view, pageYPos = 0f)
        } else {
            uploadNextPage(visiblePages.last())

            uploadPrevPage(visiblePages.first())
        }
        log("After uploadNewPages")
        dump()
    }

    private fun uploadPrevPage(page: PageView, addIfAbsent: Boolean = false) {
        if (page.pageNum <= 0) return
        val pageNum = page.pageNum - 1
        if (addIfAbsent && visiblePages.any { it.pageNum == pageNum }) return

        val pageStart = page.layoutData.position.y
        if (pageStart > 5 || addIfAbsent) {
            val newView = controller.createCachePageView(pageNum)
            addPageInPosition(
                newView,
                0f,
                page.layoutData.position.y - newView.layoutData.wholePageRect.height() - 2,
                false
            )
        }
    }

    private fun uploadNextPage(page: PageView, addIfAbsent: Boolean = false) {
        val pageNum = page.pageNum + 1
        if (addIfAbsent && visiblePages.any { it.pageNum == pageNum }) return

        val pageEnd =
            page.layoutData.position.y + page.layoutData.wholePageRect.height() + 1
        if (pageEnd < sceneHeight || addIfAbsent) {
            if (pageNum < controller.pageCount) {
                addPageInPosition(pageNum, pageEnd)
            }
        }
    }

    private fun addPageInPosition(pageNum: Int, pageYPos: Float) {
        val view = controller.createCachePageView(pageNum)
        addPageInPosition(view, pageYPos = pageYPos)
    }

    private fun addPageInPosition(view: PageView, pageXPos: Float = 0f, pageYPos: Float, last: Boolean = true) {
        view.layoutData.position.y = pageYPos
        view.layoutData.position.x = pageXPos
        if (last) {
            visiblePages.add(view)
        } else {
            visiblePages.add(0, view)
        }
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
        val visible = this.isVisible
        if (!visible) {
            log("toInvisible ${this.pageNum}: ${this.layoutData.globalRectInTmp()} $sceneRect")
            this.toInvisible()
        }
        return visible
    }

    fun findPageAndPageRect(screenRect: Rect): List<PageAndSelection> {
        return visiblePages.mapNotNull {
            val visibleOnScreenPart = it.layoutData.occupiedScreenPartInTmp(screenRect) ?: return@mapNotNull null
            log("selection: " + it.pageNum + visibleOnScreenPart)
            val pageSelection = Rect(visibleOnScreenPart)
            //TODO zoom and crop
            pageSelection.minusOffset(it.layoutData.position)
            val layoutInfo = it.layoutInfo
            val zoom  = layoutInfo.docZoom
            pageSelection.offset(layoutInfo.x.marginLess, layoutInfo.y.marginLess)
            pageSelection.set((pageSelection.left/zoom).toInt(),
                (pageSelection.top/zoom).toInt(),
                (pageSelection.right/zoom).toInt(), (pageSelection.bottom/zoom).toInt()
            )
            log("selection: " + it.pageNum + pageSelection)
            PageAndSelection(it.pageNum, pageSelection)
        }
    }

    fun renderVisiblePages(canvas: Canvas, scene: OrionDrawScene) {
        visiblePages.forEach {
            if (it.isVisible) {
                it.draw(canvas, scene)
                if (isSinglePageMode) return
            }
        }
    }

    fun renderNextOrPrev(next: Boolean): Deferred<PageView?>? {
        currentPageLayout()?.let {
            val copy = it.copy()
            val layoutStrategy = controller.layoutStrategy
            when (val res = layoutStrategy.calcPageLayout(copy, next)) {
                0 -> {
                    return controller.drawPage(copy)
                }

                1 ->
                    return renderPageAt(copy.pageNumber + 1, 0, 0) { page ->
                        val pos = page.layoutInfo.copy()
                        layoutStrategy.reset(pos, page.page, next)
                        val newPosition = page.layoutData.position
                        val x = -pos.x.offset
                        val y = -pos.y.offset
                        doScrollOnly(newPosition.x, newPosition.y, x - newPosition.x, y - newPosition.y)
                    }
                -1 ->
                    if (copy.pageNumber > 0) {
                        return renderPageAt(copy.pageNumber - 1, 0, 0) { page ->
                            val pos = page.layoutInfo.copy()
                            layoutStrategy.reset(pos, page.page, next)
                            val newPosition = page.layoutData.position
                            val x = -pos.x.offset
                            val y = -pos.y.offset
                            doScrollOnly(newPosition.x, newPosition.y, x - newPosition.x, y - newPosition.y)
                        }
                    }

                else -> throw RuntimeException("Unknown result $res")
            }
        }
        return null
    }

    fun renderPageAt(pageNum: Int, x: Int, y: Int, doScroll: (PageView) -> Unit = {page ->
        val newPosition = page.layoutData.position
        doScrollOnly(newPosition.x, newPosition.y, x - newPosition.x, y - newPosition.y)
    }): Deferred<PageView?> {
        isSinglePageMode = true
        val iterator = visiblePages.iterator()
        for (page in iterator) {
            if (page.pageNum != pageNum) {
                //TODO optimize
                page.toInvisible()
                page.destroy()
                iterator.remove()
            }
        }
        val page = if (visiblePages.isNotEmpty()) {
            visiblePages.first()
        } else {
            val page = controller.createCachePageView(pageNum)
            addPageInPosition(page, -x.toFloat(), -y.toFloat())
            page
        }

        return GlobalScope.async (Dispatchers.Main + handler) {
            page.pageInfo.await()
            if (isVisible(page)) {
                doScroll(page)
                page.renderVisible()?.await()
            }
            else null
        }
    }

    private fun dump() {
        visiblePages.forEach{
            log("Dump ${it.pageNum}: ${it.layoutData.position} ${it.layoutData.wholePageRect}")
        }

        visiblePages.zipWithNext().forEach {
            val rect1 = it.first.layoutData.globalRectInTmp()
            val rect2 = it.second.layoutData.globalRectInTmp()
            if (rect1.intersect(rect2)) {
                log("intersection area: $rect1")
                log("intersection rects: ${it.first.layoutData.globalRectInTmp()} $rect2")
                error("intersected pages ${it.first.pageNum} and ${it.second.pageNum} rects: $rect1")
            }
        }
    }

    fun isVisible(view: PageView): Boolean {
        return isVisible(view.wholePageRect, view.layoutData.position)
    }

    fun isVisible(pageRect: Rect, pos: PointF): Boolean {
        tmpRect.set(pageRect)
        tmpRect.offset(pos.x.toInt(), pos.y.toInt())
        return Rect.intersects(tmpRect, sceneRect)
    }

    fun onPageSizeCalculated(updatedView: PageView, oldArea: Rect) {
        log("onSizeCalculated ${updatedView.pageNum}: ${updatedView.layoutData} $oldArea")
        val delta = updatedView.wholePageRect.height() - oldArea.height()

        var found = false
        if (updatedView.layoutData.position.y < 0) {
            for (page in visiblePages) {
                page.layoutData.position.y -= delta
                page.updateState()
                if (updatedView === page) break
            }
        } else {
            for (page in visiblePages) {
                if (found) {
                    page.layoutData.position.y += delta
                    page.updateState()
                } else if (page === updatedView) {
                    found = true
                }
            }
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