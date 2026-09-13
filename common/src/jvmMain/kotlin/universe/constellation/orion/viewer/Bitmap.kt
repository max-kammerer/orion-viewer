package universe.constellation.orion.viewer

/* Enough of a bitmap for the document model to compile and for tests that never draw. */
actual class Bitmap(private val width: Int, private val height: Int) {
    actual fun getWidth(): Int = width
    actual fun getHeight(): Int = height
    actual open fun getPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {}
}

actual fun createBitmap(width: Int, height: Int): Bitmap = Bitmap(width, height)

/** Access-ordered map capped at [maxSize] entries, the semantics of androidx LruCache with size 1 per entry. */
actual class LruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maxSize
    }

    actual fun evictAll() = map.clear()
    actual operator fun get(k: K): V? = map[k]
    actual fun put(k: K, v: V): V? = map.put(k, v)
}

actual fun <K, V> createCache(size: Int): LruCache<K, V> = LruCache(size)
