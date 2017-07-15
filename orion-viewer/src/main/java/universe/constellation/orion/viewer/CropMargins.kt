package universe.constellation.orion.viewer

data class AutoCropMargins(
        @JvmField val left: Int,
        @JvmField val top: Int,
        @JvmField val right: Int,
        @JvmField val bottom: Int)

data class CropMargins(
        @JvmField val left: Int = 0,
        @JvmField val right: Int = 0,
        @JvmField val top: Int = 0,
        @JvmField val bottom: Int = 0,
        @JvmField val evenLeft: Int = 0,
        @JvmField val evenRight: Int = 0,
        @JvmField val evenCrop: Boolean = false,
        @JvmField val cropMode: Int = CropMode.MANUAL.cropMode)

enum class CropMode(@JvmField val cropMode: Int) {
    NO_MODE(-1),
    MANUAL(0),
    AUTO(1),
    MANUAL_AUTO(2),
    AUTO_MANUAL(3)
}


val Int.toMode: CropMode
    get() = when (this) {
        -1 -> CropMode.NO_MODE
        0 -> CropMode.MANUAL
        1 -> CropMode.AUTO
        2 -> CropMode.MANUAL_AUTO
        3 -> CropMode.AUTO_MANUAL
        else -> throw RuntimeException("Unknown mode $this")
    }

fun CropMode.isManualFirst() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO
fun CropMode.hasManual() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.isAutoFirst() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.hasAuto() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL || this == CropMode.MANUAL_AUTO