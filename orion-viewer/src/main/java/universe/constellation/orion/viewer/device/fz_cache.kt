package universe.constellation.orion.viewer.device

const val M_256_MB = 256L shl 20
const val M_512_MB = 512L shl 20
const val M_1024_MB = 1024L shl 20
const val M_1536_MB = 1536L shl 20
const val M_2048_MB = 2048L shl 20

fun calcFZCacheSize(deviceMemory: Long): Long {
    return when {
        deviceMemory <= M_256_MB -> 48L
        deviceMemory <= M_512_MB -> 64L
        deviceMemory <= M_1024_MB -> 96L
        deviceMemory <= M_1536_MB -> 128L
        deviceMemory <= M_2048_MB -> 160L
        else -> 256
    } shl 20
}