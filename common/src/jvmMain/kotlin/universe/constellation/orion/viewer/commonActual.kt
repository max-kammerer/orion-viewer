package universe.constellation.orion.viewer

actual val isDebugBuild: Boolean = true

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual inline fun <R> Any.mySynchronized(p: () -> R): R = synchronized(this) { p() }
