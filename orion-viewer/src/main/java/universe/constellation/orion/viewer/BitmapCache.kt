package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Color
import java.util.concurrent.ConcurrentLinkedQueue

private const val DEFAULT_BITMAP_CACHE_SIZE = 32
private const val CLEAN_THREASHHOLD = 5

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
                    if (BuildConfig.DEBUG && cachedBitmaps.size >= size) error("cache is full: ${cachedBitmaps.joinToString{"" + it.owned}}")
                } else {
                    cachedBitmaps.remove(info)
                }
            }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            log("BitmapCache: new bitmap $width x $height created")
            cachedBitmaps.add(CacheInfo(bitmap))
        } else {
            log("BitmapCache: using cached bitmap $bitmap")
        }
        bitmap.eraseColor(Color.TRANSPARENT)
        log("Cache size ${cachedBitmaps.size}")
        return bitmap
    }

    fun markFree(info: Bitmap) {
        for (next in cachedBitmaps) {
            if (next.bitmap === info) {
                next.owned = false
                break
            }
        }
    }

    fun cleanIfNeeded() {
        var toClean = cachedBitmaps.count { !it.owned } - CLEAN_THREASHHOLD
        if (toClean > 0) {
            val it = cachedBitmaps.iterator()
            while (it.hasNext() && toClean >= 0) {
                if (!it.next().owned) {
                    it.remove()
                    toClean--
                }
            }
        }
    }

    private fun invalidateCache() {
        for (next in cachedBitmaps) {
            next.owned = false
        }
        log("BitmapCache: cache invalidated")
    }

    fun free() {
        invalidateCache()
        cachedBitmaps.clear()
    }
}
