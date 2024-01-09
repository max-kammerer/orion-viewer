package universe.constellation.orion.viewer

class LruCacheWithOnEvict<K, V>(size: Int, val onEvict: (V) -> Unit) : LruCache<K, V>(size) {
    override fun entryRemoved(evicted: Boolean, key: K & Any, oldValue: V & Any, newValue: V?) {
        onEvict(oldValue)
    }
}