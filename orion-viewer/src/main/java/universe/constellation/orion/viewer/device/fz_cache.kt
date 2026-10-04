package universe.constellation.orion.viewer.device

const val M_256_MB = 256L shl 20
const val M_512_MB = 512L shl 20
const val M_1024_MB = 1024L shl 20
const val M_1536_MB = 1536L shl 20
const val M_2048_MB = 2048L shl 20

const val M_4096_MB = 4096L shl 20

fun calcFZCacheSize(deviceMemory: Long): Long {
    return when {
        deviceMemory <= M_256_MB -> 48L
        deviceMemory <= M_512_MB -> 64L
        deviceMemory <= M_1024_MB -> 96L
        deviceMemory <= M_1536_MB -> 128L
        deviceMemory <= M_2048_MB -> 160L
        deviceMemory <= M_4096_MB -> 256L
        else -> 256L + 128L
    } shl 20
}

/* libdjvu caches whole decoded page files (JB2 shapes, IW44 coefficients) and the shared
 * dictionaries: a scanned page takes 0.7-2 MB and the cache undercounts it by about a third.
 * A quarter of the mupdf store keeps a handful of pages without competing with the bitmaps. */
fun calcDjvuCacheSize(deviceMemory: Long): Long = calcFZCacheSize(deviceMemory) / 2

/* Limit (in bytes of the decoded pixmap, after l2factor downsampling) up to which page
 * images are decoded and cached whole for tile reuse, see orion_image_decode in orion_bitmap.c.
 * A 600 dpi A4 scan is ~35 MB at 1 byte/pixel, the same color page three times that. */
fun calcFullImageDecodeBytes(deviceMemory: Long): Long {
    return when {
        deviceMemory <= M_1024_MB -> 16L
        deviceMemory <= M_2048_MB -> 24L
        else -> 40L
    } shl 20
}
