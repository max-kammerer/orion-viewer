package universe.constellation.orion.viewer.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.log
import kotlin.coroutines.coroutineContext

data class Key(val x: Int, val y: Int) : Comparable<Key> {
    override fun compareTo(other: Key): Int {
        val res = x.compareTo(other.x)
        return if (res != 0) res else y.compareTo(other.y)
    }
}

data class PagePart(val absPartRect: Rect) {

    @Volatile
    var bitmapInfo: BitmapCache.CacheInfo? = null

    @Volatile
    internal var isActive: Boolean = false

    //access from UI thread
    private val localPartRect = Rect(0, 0, absPartRect.width(), absPartRect.height())

    //access from renderer thread
    private val rendTmp = Rect()

    @Volatile
    private var nonRenderedPart = Region(absPartRect)
    private var nonRenderedPartTmp = Region(absPartRect)

    @Volatile
    private var opMarker = 1

    suspend fun render(
        requestedArea: Rect,
        zoom: Double,
        cropLeft: Int,
        cropTop: Int,
        page: Page
    ) {
        if (!isActive) return

        val info: BitmapCache.CacheInfo
        val marker = synchronized(this) {
            if (nonRenderedPart.isEmpty || !coroutineContext.isActive) return
            //TODO add busy flag
            info = this.bitmapInfo ?: return
            opMarker
        }

        info.mutex.lock()
        try {
            if (marker != opMarker || !coroutineContext.isActive) return

            //TODO: join bitmap
            rendTmp.set(absPartRect)
            if (rendTmp.intersect(requestedArea)) {
                nonRenderedPartTmp.set(nonRenderedPart)
                if (nonRenderedPartTmp.op(rendTmp, Region.Op.INTERSECT)) {
                    rendTmp.set(nonRenderedPartTmp.bounds)
                    rendTmp.offset(-absPartRect.left, -absPartRect.top)
                    renderInner(
                        rendTmp,
                        zoom,
                        absPartRect.left + cropLeft,
                        absPartRect.top + cropTop,
                        page,
                        info.bitmap
                    )
                    synchronized(this) {
                        if (opMarker == marker) {
                            rendTmp.offset(absPartRect.left, absPartRect.top)
                            nonRenderedPart.op(rendTmp, Region.Op.DIFFERENCE)
                        }
                    }
                }
            }
        } finally {
            info.mutex.unlock()
        }
    }

    fun draw(canvas: Canvas, pageVisiblePart: Rect, defaultPaint: Paint) {
        if (!isActive || bitmapInfo == null) return
        if (Rect.intersects(absPartRect, pageVisiblePart)) {
            canvas.drawBitmap(bitmapInfo!!.bitmap, localPartRect, absPartRect, defaultPaint)
        }
    }

    internal fun activate(bitmapCache: BitmapCache, partWidth: Int, partHeight: Int) {
        assert(!isActive)
        synchronized(this) {
            isActive = true
            opMarker++
            bitmapInfo = bitmapCache.createBitmap(partWidth, partHeight)
            nonRenderedPart.set(absPartRect)
        }
    }

    internal fun deactivate(bitmapCache: BitmapCache) {
        synchronized(this) {
            isActive = false
            opMarker++
            bitmapCache.free(bitmapInfo ?: return)
            bitmapInfo = null
        }
    }
}

class FlexibleBitmap(width: Int, height: Int, val partWidth: Int, val partHeight: Int, val pageNum: Int = -1) {

    constructor(initialArea: Rect, partWidth: Int, partHeight: Int) : this(
        initialArea.width(),
        initialArea.height(),
        partWidth,
        partHeight
    )

    private var renderingArea = Rect(0, 0, width, height)

    @Volatile
    var data = initData(renderingArea.width(), renderingArea.height())
        private set

    val width: Int
        get() = renderingArea.width()

    val height: Int
        get() = renderingArea.height()

    private fun initData(width: Int, height: Int): MutableMap<Key, PagePart> {
        log("FB: initData $pageNum $width $height")
        return sortedMapOf()
    }

    fun resize(width: Int, height: Int, bitmapCache: BitmapCache): FlexibleBitmap {
        log("FB: resize $pageNum $width $height")
        free(bitmapCache)
        data = initData(width, height)
        renderingArea.set(0, 0, width, height)

        return this
    }

    fun enableAll(cache: BitmapCache) {
        updateDrawAreaAndUpdateNonRenderingPart(renderingArea, cache)
    }

    fun disableAll(cache: BitmapCache) {
        data.values.forEach { it.deactivate(cache) }
    }

    fun updateDrawAreaAndUpdateNonRenderingPart(activationRect: Rect, cache: BitmapCache) {
        val rect = Rect(activationRect)
        val isEmptyIntersection = !rect.intersect(renderingArea)

        val left = rect.left.countCellInc(partWidth)
        val right = rect.right.countCell(partWidth)

        val top = rect.top.countCellInc(partHeight)
        val bottom = rect.bottom.countCell(partHeight)

        //free bitmaps
        val it = data.iterator()
        while (it.hasNext()) {
                val (key, value) = it.next()
                val (x, y) = key
                if (isEmptyIntersection || x !in left..right || y !in top..bottom) {
                    if (value.isActive) {
                        value.deactivate(cache)
                    }

                    if (x !in left - 2..right + 2 || y !in top - 2..bottom + 2) {
                        synchronized(data) {
                            it.remove()
                        }
                    }
                }
        }


        //then enable visible ones
        if (!isEmptyIntersection) {
            for (col in left..right) {
                for (row in top..bottom) {
                    val key = Key(col, row)
                    var page = data[key]
                    if (page == null) {
                        page = PagePart(
                            Rect(
                                col * partWidth,
                                row * partHeight,
                                (col + 1) * partWidth,
                                (row + 1) * partHeight
                            )
                        )
                        synchronized(data) {
                            data.put(key, page)
                        }
                    }
                    if (!page.isActive) {
                        page.activate(cache, partWidth, partHeight)
                    }
                }
            }
        }
    }

    @TestOnly
    suspend fun renderFull(zoom: Double, page: Page) {
        coroutineScope {
            forEach(true) {
                launch {
                    render(
                        renderingArea,
                        zoom,
                        0,
                        0,
                        page,
                    )
                }
            }
        }
    }

    suspend fun render(renderingArea: Rect, curPos: LayoutPosition, page: Page) {
        log("FB rendering $page $renderingArea")
        coroutineScope {
            forEach(true) {
                if (isActive) {
                    launch {
                        render(
                            renderingArea,
                            curPos.docZoom,
                            curPos.x.marginLeft,
                            curPos.y.marginLeft,
                            page,
                        )
                    }
                }
            }
        }
    }

    fun forAllTest(body: PagePart.() -> Unit) {
        forEach(body = body)
    }

    private inline fun forEach(copy: Boolean = false, body: PagePart.() -> Unit) {
        val data = if (copy) synchronized(data)  { ArrayList(data.values) } else data.values
        data.forEach { part ->
            part.body()
        }
    }

    fun draw(canvas: Canvas, srcRect: Rect, defaultPaint: Paint) {
        forEach { draw(canvas, srcRect, defaultPaint) }
    }

    fun free(cache: BitmapCache) {
        forEach { deactivate(cache) }
    }

    @TestOnly
    fun bitmaps(): List<Bitmap> {
        val res = mutableListOf<Bitmap>()
        forEach { bitmapInfo?.bitmap?.let { res.add(it) } }
        return res
    }
}

private fun Int.countCellInc(cellSize: Int): Int {
    if (this == 0 || cellSize == 0) return 0
    return this / cellSize
}

private fun Int.countCell(cellSize: Int): Int {
    if (this == 0 || cellSize == 0) return 0
    return (this - 1) / cellSize
}

private fun renderInner(bound: Rect, zoom: Double, offsetX: Int, offsetY: Int, page: Page, bitmap: Bitmap): Bitmap {
    log("Rendering $page: $bound\n $offsetX $offsetY\nbm=${bitmap.width}x${bitmap.height}\ndim${bound.width()} ${bound.height()}")
    page.renderPage(
        bitmap,
        zoom,
        bound.left,
        bound.top,
        bound.right,
        bound.bottom,
        offsetX,
        offsetY

    )
    return bitmap
}