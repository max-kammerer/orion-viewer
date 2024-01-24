package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Color
import java.util.concurrent.ConcurrentLinkedQueue

private const val DEFAULT_BITMAP_CACHE_SIZE = 10

open class BitmapCache(val size: Int = DEFAULT_BITMAP_CACHE_SIZE) {

    private val cachedBitmaps = ConcurrentLinkedQueue<CacheInfo>()

    protected class CacheInfo(val bitmap: Bitmap) {
        var isValid = true
    }

    fun createBitmap(width: Int, height: Int): Bitmap {
        var bitmap: Bitmap? = null
        if (cachedBitmaps.size >= size) {
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
            log("BitmapCache: new bitmap $width x $height created")
        } else {
            log("BitmapCache: using cached bitmap $bitmap")
        }
        addToCache(CacheInfo(bitmap))
        bitmap.eraseColor(Color.TRANSPARENT)

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

    fun free() {
        invalidateCache()
        cachedBitmaps.map { it.bitmap.recycle() }
        cachedBitmaps.clear()
    }
}
