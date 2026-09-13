package universe.constellation.orion.viewer

actual val isDebugBuild: Boolean = universe.constellation.orion.common.BuildConfig.DEBUG

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual inline fun <R> Any.mySynchronized(p: () -> R): R {
    return synchronized(this) {
        p()
    }
}