package universe.constellation.orion.viewer.bitmap

import kotlin.math.max

class DeviceInfo(
    val heapMemoryInMB: Int,
    val width: Int,
    val height: Int,
    val deviceUseNativeBitmap: Boolean
) {

    val screenBitmapSizeInBytes = width * height * 4

    val heapMemoryInBytes = heapMemoryInMB * 1024 * 1024

    fun visiblePageLimit(): Int {
        if (deviceUseNativeBitmap) {
            return 3
        } else {
            val screens = screensByMemory()
            if (screens < 9) return 2
            return 3
        }
    }

    fun maxScreensDirection(): Int {
        if (deviceUseNativeBitmap) {
            return 3
        } else {
            if (screensByMemory() < 9) {
                return 2
            } else {
                return 3
            }

        }
    }

    private fun screensByMemory(): Int {
        val halfMemory = heapMemoryInBytes / 2
        return max(4, halfMemory / screenBitmapSizeInBytes)
    }

}