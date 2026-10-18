package universe.constellation.orion.viewer

import java.io.File

/**
 * The first [count] bytes of the file as ASCII, other bytes as '.': enough to tell one format from
 * another in a problem report, e.g. a web page or an image saved under a book's name. Shorter
 * for a shorter file, empty when it can't be read.
 */
fun readFileHeader(path: String, count: Int): String = printable(readFileBytes(path, count))

/** The first [count] bytes of the file, fewer for a shorter file, none when it can't be read. */
private fun readFileBytes(path: String, count: Int): ByteArray {
    val bytes = ByteArray(count)
    val read = try {
        File(path).inputStream().use { it.read(bytes) }.coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
    return bytes.copyOf(read)
}

private fun isPrintable(b: Byte) = b.toInt().toChar() in ' '..'~'

private fun printable(bytes: ByteArray): String =
    bytes.joinToString("") { b -> if (isPrintable(b)) b.toInt().toChar().toString() else "." }

private fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

private val DJVU_FORMS = setOf("DJVM", "DJVU", "DJVI", "THUM")

/**
 * The start of a file for a problem report: the first 8 bytes, for an IFF file (DjVu) its
 * form type, which follows the 4-byte size that reads as noise, and what the bytes are, see
 * [detectFileContent]. Tells what arrived under a book's name: a web page, an image, a PDF
 * named .djvu, encrypted or zeroed data. A form type that isn't DjVu's comes in hex when not
 * printable, with the end of the form its size declares and the file length.
 */
fun describeFileHeader(path: String): String {
    val bytes = readFileBytes(path, 16)
    val header = printable(bytes)
    // where the 4-byte size of the top FORM chunk starts: after "AT&T" in DjVu, at 4 in plain IFF
    val sizeAt = when {
        header.startsWith("AT&TFORM") && bytes.size >= 16 -> 8
        header.startsWith("FORM") && bytes.size >= 12 -> 4
        else -> null
    }
    val parts = mutableListOf("header=${header.take(8)}")
    if (sizeAt != null) {
        val formBytes = bytes.copyOfRange(sizeAt + 4, sizeAt + 8)
        val form = printable(formBytes)
        // a damaged header: hex tells zeros from noise, the declared size tells a cut file
        if (form !in DJVU_FORMS) {
            val size = (0 until 4).fold(0L) { acc, i -> (acc shl 8) or (bytes[sizeAt + i].toLong() and 0xFF) }
            parts += "form=${if (formBytes.all(::isPrintable)) form else hex(formBytes)}"
            parts += "form_end=${size + sizeAt + 4}"
            parts += "file_length=${File(path).length()}"
        } else {
            parts += "form=$form"
        }
    }
    detectFileContent(path)?.let { parts += "content=${it.name.lowercase()}" }
    return parts.joinToString(", ")
}

/**
 * Whether [marker] occurs in the last [window] bytes of the file: true, false, or null when the
 * file can't be read. For a PDF, a missing `%%EOF` there means the end of the file is gone, an
 * incomplete download or copy, as the cross-reference table and often the page tree sit last.
 */
fun fileTailContains(path: String, marker: String, window: Int = 1024): Boolean? = try {
    java.io.RandomAccessFile(path, "r").use { file ->
        val length = file.length()
        val size = minOf(window.toLong(), length).toInt()
        val bytes = ByteArray(size)
        file.seek(length - size)
        file.readFully(bytes)
        String(bytes, Charsets.ISO_8859_1).contains(marker)
    }
} catch (e: Exception) {
    null
}
