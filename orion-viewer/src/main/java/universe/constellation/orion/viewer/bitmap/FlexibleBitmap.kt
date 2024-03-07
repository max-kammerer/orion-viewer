package universe.constellation.orion.viewer.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import org.jetbrains.annotations.TestOnly
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.log
import kotlin.math.min

data class PagePart(val absPartRect: Rect) {

    @Volatile
    var bitmap: Bitmap? = null

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

    fun render(
        requestedArea: Rect,
        zoom: Double,
        cropLeft: Int,
        cropTop: Int,
        page: Page
    ) {
        if (!isActive) return

        val bitmap: Bitmap
        val marker = synchronized(this) {
            if (nonRenderedPart.isEmpty) return
            //TODO add busy flag
            bitmap = this.bitmap ?: return
            opMarker
        }

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
                    bitmap
                )
                synchronized(this) {
                    if (opMarker == marker) {
                        rendTmp.offset(absPartRect.left, absPartRect.top)
                        nonRenderedPart.op(rendTmp, Region.Op.DIFFERENCE)
                    }
                }
            }
        }
    }

    fun draw(canvas: Canvas, pageVisiblePart: Rect, defaultPaint: Paint) {
        if (bitmap != null && localPartRect.intersect(pageVisiblePart)) {
            canvas.drawBitmap(bitmap!!, localPartRect, absPartRect, defaultPaint)
        }
    }

    internal fun activate(bitmapCache: BitmapCache, partWidth: Int, partHeight: Int) {
        assert(!isActive)
        synchronized(this) {
            isActive = true
            opMarker++
            bitmap = bitmapCache.createBitmap(partWidth, partHeight)
            nonRenderedPart.set(absPartRect)
        }
    }

    internal fun deactivate(bitmapCache: BitmapCache) {
        synchronized(this) {
            isActive = false
            opMarker++
            bitmap?.let {
                bitmapCache.free(it)
            }
            bitmap = null
        }
    }
}

class FlexibleBitmap(width: Int, height: Int, val partWidth: Int, val partHeight: Int) {

    constructor(initialArea: Rect, partWidth: Int, partHeight: Int): this(initialArea.width(), initialArea.height(), partWidth, partHeight)

    private var renderingArea = Rect(0, 0, width, height)

    var data = initData(renderingArea.width(), renderingArea.height())
        private set

    val width: Int
        get() =  renderingArea.width()

    val height: Int
        get() =  renderingArea.height()

    private fun initData(width: Int, height: Int): Array<Array<PagePart>> {
        val rowCount = height.countCells(partHeight)
        return Array(rowCount) { row ->
            val colCount = width.countCells(partWidth)
            Array(colCount) { col ->
                val left = partWidth * col
                val top = partHeight * row
                val partWidth = min(width - left, partWidth)
                val partHeight = min(height - top, partHeight)
                PagePart(
                    Rect(
                        left,
                        top,
                        left + partWidth,
                        top + partHeight
                    )
                )
            }
        }
    }

    fun resize(width: Int, height: Int, bitmapCache: BitmapCache): FlexibleBitmap {
        free(bitmapCache)
        data = initData(width, height)
        renderingArea.set(0, 0, width, height)

        return this
    }

    fun enableAll(cache: BitmapCache) {
        updateDrawAreaAndUpdateNonRenderingPart(renderingArea, cache)
    }

    fun disableAll(cache: BitmapCache) {
        updateDrawAreaAndUpdateNonRenderingPart(Rect(-1, -1, -1, -1), cache)
    }

    fun updateDrawAreaAndUpdateNonRenderingPart(activationRect: Rect, cache: BitmapCache) {
        //free out of view bitmaps
        forAll {
            val newState = Rect.intersects(this.absPartRect, activationRect)
            if (isActive && !newState) {
                deactivate(cache)
            }
        }
        //then enable visible ones
        forAll {
            val newState = Rect.intersects(this.absPartRect, activationRect)
            if (!isActive && newState) {
                activate(cache, partWidth, partHeight)
            }
        }
        var count = 0
        forAll {

            if (isActive ) {
                count++
            }
        }
//        println("Cache active parts " + count)
//        if (count == 20) {
//            print("Cache part $activationRect ${activationRect.width()} ${activationRect.height()}")
////            forAll {
////                if (isActive) {
////                    print("" + this.absPartRect + ", ")
////                }
////            }
//            println("")
//        }
    }

    @TestOnly
    fun renderFull(zoom: Double, page: Page) {
        forEach(renderingArea) {
            render(
                renderingArea,
                zoom,
                0,
                0,
                page,
            )
        }
    }

    fun render(renderingArea: Rect, curPos: LayoutPosition, page: Page) {
        forEach(renderingArea) {
            render(
                renderingArea,
                curPos.docZoom,
                curPos.x.marginLess,
                curPos.y.marginLess,
                page,
            )
        }
    }

    fun forAllTest(body: PagePart.() -> Unit) {
        forAll(body)
    }

    private inline fun forAll(body: PagePart.() -> Unit) {
        if (partWidth == 0 || partHeight == 0) {
            data.forEach {
                it.forEach { part -> part.body() }
            }
        } else {
            forEach(renderingArea, body)
        }
    }

    private inline fun forEach(rect: Rect, body: PagePart.() -> Unit) {
        val left = rect.left / partWidth
        val top = rect.top / partHeight
        val right = rect.rightInc / partWidth
        val bottom = rect.bottomInc / partHeight
        for (r in top..bottom) {
            for (c in left..right) {
                data[r][c].body()
            }
        }
    }

    fun draw(canvas: Canvas, srcRect: Rect, defaultPaint: Paint) {
        forAll {
            draw(canvas, srcRect, defaultPaint)
        }
    }

    fun free(cache: BitmapCache) {
        forAll {
            deactivate(cache)
        }
    }

    fun bitmaps(): List<Bitmap> {
        val res = mutableListOf<Bitmap>()
        forAll {
            bitmap?.let { res.add(it) }
        }
        return res
    }

    fun parts(): List<PagePart> {
        val res = mutableListOf<Bitmap>()
        return data.flatMap { it.map { it } }
    }
}

fun Int.countCells(cellSize: Int): Int {
    if (this == 0) return 0
    return (this - 1) / cellSize + 1
}

private val Rect.rightInc get() = right - 1
private val Rect.bottomInc get() = bottom - 1

private fun renderInner(bound: Rect, zoom: Double, offsetX: Int, offsetY: Int, page: Page, bitmap: Bitmap): Bitmap {
    log("Rendering $page: $bound $offsetX $offsetY")
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