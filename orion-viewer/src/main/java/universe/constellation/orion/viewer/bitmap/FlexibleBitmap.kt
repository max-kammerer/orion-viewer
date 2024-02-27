package universe.constellation.orion.viewer.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.log

data class PagePart(val absPartRect: Rect) {

    @Volatile
    var bitmap: Bitmap? = null

    @Volatile
    internal var isActive: Boolean = false

    //access from UI thread
    private var drawTmp = Rect()
    private var sceneTmp = RectF()

    //access from renderer thread
    private var rendTmp = Rect()
    private var nonRenderedPart = Region(absPartRect)
    private var nonRenderedPartTmp = Region(absPartRect)

    fun render(
        requestedArea: Rect,
        zoom: Double,
        cropLeft: Int,
        cropTop: Int,
        page: Int,
        doc: Document,
        bitmapCache: BitmapCache
    ) {
        if (!isActive) return
        if (nonRenderedPart.isEmpty) return

        //TODO add busy flag
        val bitmap = bitmap ?: return
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
                    doc,
                    bitmap
                )
                rendTmp.offset(absPartRect.left, absPartRect.top)
                nonRenderedPart.op(rendTmp, Region.Op.DIFFERENCE)
            }
        }
    }

    fun draw(canvas: Canvas, pageVisiblePart: Rect, scene: RectF, defaultPaint: Paint) {
        drawTmp.set(absPartRect)
        if (bitmap != null && drawTmp.intersect(pageVisiblePart)) {
            val deltaX = -(pageVisiblePart.left - scene.left)
            val deltaY = -(pageVisiblePart.top - scene.top)
            sceneTmp.set(drawTmp)
            sceneTmp.offset(deltaX, deltaY)
            drawTmp.offset(-absPartRect.left, -absPartRect.top)
            canvas.drawBitmap(bitmap!!, drawTmp, sceneTmp, defaultPaint)
            log("DrawPart: $drawTmp $sceneTmp")
        }
    }

    internal fun activate(bitmapCache: BitmapCache) {
        isActive = true
        bitmap = bitmapCache.createBitmap(absPartRect.width(), absPartRect.height())
    }

    internal fun deactivate(bitmapCache: BitmapCache) {
        isActive = false
        bitmap?.let {
            bitmapCache.free(it)
        }
        bitmap = null
        nonRenderedPart.set(absPartRect)
    }
}

class FlexibleBitmap(initialArea: Rect, val partWidth: Int, val partHeight: Int) {
    private var renderingArea = initialArea
        private set

    private var data = initData(renderingArea.width(), renderingArea.height())

    val width: Int
        get() =  renderingArea.width()

    val height: Int
        get() =  renderingArea.height()

    private fun initData(width: Int, height: Int) = Array(height / partHeight + 1) { row ->
        Array(width / partWidth + 1) { col ->
            val left = partWidth * col
            val top = partHeight * row
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

    fun resize(width: Int, height: Int, bitmapCache: BitmapCache): FlexibleBitmap {
        free(bitmapCache)
        data = initData(width, height)
        renderingArea.set(0, 0, width, height)
        return this
    }

    fun updateDrawAreaAndUpdateNonRenderingPart(activationRect: Rect, cache: BitmapCache) {
        forAll {
            val newState = Rect.intersects(this.absPartRect, activationRect)
            if (isActive) {
                if (!newState) {
                    deactivate(cache)
                }
            } else {
                if (newState) {
                    activate(cache)
                }
            }
        }
    }

    fun render(renderingArea: Rect, curPos: LayoutPosition, page: Int, doc: Document, bitmapCache: BitmapCache) {
        forEach(renderingArea) {
            render(
                renderingArea,
                curPos.docZoom,
                curPos.x.marginLess,
                curPos.y.marginLess,
                page,
                doc,
                bitmapCache
            )
        }
    }

    private inline fun forAll(body: PagePart.() -> Unit) {
        forEach(renderingArea, body)
    }

    private inline fun forEach(rect: Rect, body: PagePart.() -> Unit) {
        val left = rect.left / partWidth
        val top = rect.top / partHeight
        val right = rect.rightInc / partWidth
        val bottom = rect.bottomInc / partHeight

        for (r in top..bottom) {
            for (c in left..right) {
                body(data[r][c])
            }
        }
    }

    fun draw(canvas: Canvas, srcRect: Rect, scene: RectF, defaultPaint: Paint, borderPaint: Paint) {
        forAll {
            draw(canvas, srcRect, scene, defaultPaint)
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

private val Rect.rightInc get() = right - 1
private val Rect.bottomInc get() = bottom - 1

private fun renderInner(bound: Rect, curPos: LayoutPosition, page: Int, doc: Document, bitmap: Bitmap): Bitmap {
    println("Rendering $page: $bound $curPos")
    doc.renderPage(page, bitmap, curPos.docZoom, bound.left, bound.top,  bound.right, bound.bottom, curPos.x.marginLess, curPos.y.marginLess)
    return bitmap
}

private fun renderInner(bound: Rect, zoom: Double, offsetX: Int, offsetY: Int, page: Int, doc: Document, bitmap: Bitmap): Bitmap {
    println("Rendering $page: $bound $offsetX $offsetY")
    doc.renderPage(
        page,
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