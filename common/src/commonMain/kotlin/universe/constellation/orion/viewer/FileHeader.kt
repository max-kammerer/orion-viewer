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

/**
 * The start of a file for a problem report: the first 8 bytes, for an IFF file (DjVu) its
 * form type, which follows the 4-byte size that reads as noise, and what the bytes are, see
 * [detectFileContent]. Tells what arrived under a book's name: a web page, an image, a PDF
 * named .djvu, encrypted or zeroed data.
 */
fun describeFileHeader(path: String): String {
    val header = readFileHeader(path, 16)
    val form = when {
        header.startsWith("AT&TFORM") && header.length >= 16 -> header.substring(12, 16)
        header.startsWith("FORM") && header.length >= 12 -> header.substring(8, 12)
        else -> null
    }
    val content = detectFileContent(path)
    return listOfNotNull(
        "header=${header.take(8)}",
        form?.let { "form=$it" },
        content?.let { "content=${it.name.lowercase()}" }
    ).joinToString(", ")
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
