package universe.constellation.orion.viewer

/**
 * Created by mike on 12/18/15.
 */

class AutoCropMargins(
        @JvmField val left: Int,
        @JvmField val top: Int,
        @JvmField val right: Int,
        @JvmField val bottom: Int)

data class CropMargins(
        @JvmField val left: Int,
        @JvmField val right: Int,
        @JvmField val top: Int,
        @JvmField val bottom: Int,
        @JvmField val evenLeft: Int,
        @JvmField val evenRight: Int,
        @JvmField val evenCrop: Boolean,
        @JvmField val cropMode: Int)

enum class CropMode(@JvmField val cropMode: Int) {
    NO_MODE(-1),
    MANUAL(0),
    AUTO(1),
    MANUAL_AUTO(2),
    AUTO_MANUAL(3)
}


val Int.toMode: CropMode
    get() {
        return when (this) {
            -1 -> CropMode.NO_MODE
            0 -> CropMode.MANUAL
            1 -> CropMode.AUTO
            2 -> CropMode.MANUAL_AUTO
            3 -> CropMode.AUTO_MANUAL
            else -> throw RuntimeException("Unknown mode $this")
        }
    }

fun CropMode.isManualBegin() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO
fun CropMode.hasManual() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.isAutoBegin() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.hasAuto() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL || this == CropMode.MANUAL_AUTO