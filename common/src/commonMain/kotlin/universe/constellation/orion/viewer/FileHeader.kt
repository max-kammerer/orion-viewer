package universe.constellation.orion.viewer

import java.io.File

/**
 * The first [count] bytes of the file as ASCII, other bytes as '.': enough to tell one format from
 * another in a problem report, e.g. a web page or an image saved under a book's name. Shorter
 * for a shorter file, empty when it can't be read.
 */
fun readFileHeader(path: String, count: Int): String {
    val bytes = ByteArray(count)
    val read = try {
        File(path).inputStream().use { it.read(bytes) }.coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
    return bytes.take(read).joinToString("") { b ->
        b.toInt().toChar().let { if (it in ' '..'~') it.toString() else "." }
    }
}
