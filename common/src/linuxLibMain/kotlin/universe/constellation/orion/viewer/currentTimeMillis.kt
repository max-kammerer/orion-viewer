package universe.constellation.orion.viewer

actual fun currentTimeMillis(): Long =
    kotlin.system.getTimeMillis()

actual inline fun <R> Any.mySynchronized(p: () -> R): R {
    //TODO
    return p()
}