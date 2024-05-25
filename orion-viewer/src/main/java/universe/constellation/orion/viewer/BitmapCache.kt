package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentLinkedQueue

private const val DEFAULT_BITMAP_CACHE_SIZE = 32
private const val CLEAN_THREASHHOLD = 5

open class BitmapCache(val size: Int = DEFAULT_BITMAP_CACHE_SIZE) {

    private val cachedBitmaps = ConcurrentLinkedQueue<CacheInfo>()

    class CacheInfo(val bitmap: Bitmap, val mutex: Mutex) {
        var owned = true

        val isBusy
            get() = mutex.isLocked
    }

    fun createBitmap(width: Int, height: Int): CacheInfo {
        var resultInfo: CacheInfo? = null
        if (cachedBitmaps.size >= size / 2) {
            //TODO: add checks
            val nonValids = cachedBitmaps.asSequence().filter { !it.owned && !it.isBusy }
            val cacheInfo =
                nonValids.firstOrNull { info -> width <= info.bitmap.width && height <= info.bitmap.height }

            if (cacheInfo != null) {
                resultInfo = cacheInfo
            } else {
                val info = nonValids.firstOrNull()
                if (info == null) {
                    if (BuildConfig.DEBUG && cachedBitmaps.size >= size) error("cache is full: ${cachedBitmaps.joinToString{"" + it.owned}}")
                } else {
                    cachedBitmaps.remove(info)
                }
            }
        }

        if (resultInfo == null) {
            resultInfo = CacheInfo(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), Mutex())
            cachedBitmaps.add(resultInfo)
        } else {
            log("BitmapCache(${cachedBitmaps.size}): using cached bitmap ${System.identityHashCode(resultInfo.bitmap)}")
        }
        resultInfo.bitmap.eraseColor(Color.TRANSPARENT)
        resultInfo.owned = true
        return resultInfo
    }

    fun free(info: CacheInfo) {
        info.owned = false
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
