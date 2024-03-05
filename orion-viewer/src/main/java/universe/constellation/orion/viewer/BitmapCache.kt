package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Color
import java.util.concurrent.ConcurrentLinkedQueue

private const val DEFAULT_BITMAP_CACHE_SIZE = 25

open class BitmapCache(val size: Int = DEFAULT_BITMAP_CACHE_SIZE) {

    private val cachedBitmaps = ConcurrentLinkedQueue<CacheInfo>()

    protected class CacheInfo(val bitmap: Bitmap) {
        var owned = true
        @Volatile
        var isBusy = false
    }

    fun createBitmap(width: Int, height: Int): Bitmap {
        var bitmap: Bitmap? = null
        if (cachedBitmaps.size >= size / 2) {
            //TODO: add checks
            val nonValids = cachedBitmaps.asSequence().filter { !it.owned && !it.isBusy }
            val cacheInfo =
                nonValids.firstOrNull { info -> width <= info.bitmap.width && height <= info.bitmap.height }

            if (cacheInfo != null) {
                bitmap = cacheInfo.bitmap
                cacheInfo.owned = true
            } else {
                val info = nonValids.firstOrNull()
                if (info == null) {
                    if (cachedBitmaps.size >= size) error("cache is full: ${cachedBitmaps.joinToString{"" + it.owned}}")
                } else {
                    cachedBitmaps.remove(info)
                    info.bitmap.recycle()
                }
            }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            log("BitmapCache: new bitmap $width x $height created")
            addToCache(CacheInfo(bitmap))
        } else {
            log("BitmapCache: using cached bitmap $bitmap")
        }
        bitmap.eraseColor(Color.TRANSPARENT)
        println("Cache size ${cachedBitmaps.size}")
        return bitmap
    }

    private fun addToCache(info: CacheInfo) {
        cachedBitmaps.add(info)
    }

    fun free(info: Bitmap) {
        for (next in cachedBitmaps) {
            if (next.bitmap === info) {
                next.owned = false
                break
            }
        }
    }

    fun invalidateCache() {
        for (next in cachedBitmaps) {
            next.owned = false
        }
        log("BitmapCache: cache invalidated")
    }

    fun free() {
        invalidateCache()
        cachedBitmaps.map { it.bitmap.recycle() }
        cachedBitmaps.clear()
    }
}
