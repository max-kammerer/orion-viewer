package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.layout.LayoutPosition
import java.util.concurrent.ConcurrentLinkedQueue

private const val BITMAP_CACHE_SIZE = 10

open class BitmapCache : Thread() {

    private val cachedBitmaps = ConcurrentLinkedQueue<CacheInfo>()

    protected class CacheInfo(val bitmap: Bitmap) {
        var isValid = true
    }

    fun createBitmap(width: Int, height: Int): Bitmap {
        var bitmap: Bitmap? = null
        if (cachedBitmaps.size >= BITMAP_CACHE_SIZE) {
            //TODO: add checks
            val nonValids = cachedBitmaps.asSequence().filter { !it.isValid }
            val cacheInfo =
                nonValids.firstOrNull { info -> width <= info.bitmap.width && height <= info.bitmap.height }

            if (cacheInfo == null) {
                val info = nonValids.firstOrNull() ?: error("cache is full")
                cachedBitmaps.remove(info)
                info.bitmap.recycle()
            } else {
                bitmap = cacheInfo.bitmap
                cacheInfo.isValid = true
            }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } else {
            log("BitmapCache: using cached bitmap $bitmap")
        }
        addToCache(CacheInfo(bitmap))
        bitmap.eraseColor(Color.TRANSPARENT)

        log("BitmapCache create bitmap: $width $height")
        return bitmap
    }

    protected fun addToCache(info: CacheInfo) {
        cachedBitmaps.add(info)
    }

    fun free(info: Bitmap) {
        for (next in cachedBitmaps) {
            if (next.bitmap === info) {
                next.isValid = false
                break
            }
        }
    }

    fun invalidateCache() {
        for (next in cachedBitmaps) {
            next.isValid = false
        }
        log("BitmapCache: cache invalidated")
    }
}

fun renderInner(bound: Rect, curPos: LayoutPosition, page: Int, doc: Document, bitmap: Bitmap): Bitmap {
    println("Rendering $page: $bound $curPos")
    doc.renderPage(page, bitmap, curPos.docZoom, bound.left, bound.top,  bound.right, bound.bottom, curPos.x.marginLess, curPos.y.marginLess)
    return bitmap
}
