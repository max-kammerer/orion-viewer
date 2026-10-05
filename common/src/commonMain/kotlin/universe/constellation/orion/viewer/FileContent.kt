package universe.constellation.orion.viewer

import java.io.File

/**
 * What a file holds, told by its bytes whatever its name says: a book that won't open is
 * often something else under a book's name (a web page, an image, an archive) or data no
 * reader can use (an encrypted or overwritten file, zeros left by an incomplete download).
 */
enum class FileContent(val shortName: String) {
    PDF("PDF"),

    /** Forms Data Format: form field values for a PDF, a PDF syntax file without pages. */
    FDF("FDF"),
    DJVU("DjVu"),
    ZIP("ZIP"),
    RAR("RAR"),
    SEVEN_ZIP("7z"),
    TAR("TAR"),
    PNG("PNG"),
    JPEG("JPEG"),
    WEBP("WebP"),
    TIFF("TIFF"),
    GIF("GIF"),
    BMP("BMP"),
    JPEG2000("JPEG 2000"),

    /** ISO media (HEIC and AVIF photos, MP4 video): images mupdf doesn't decode. */
    ISO_MEDIA("HEIC/AVIF/MP4"),
    HTML("HTML"),
    XML("XML"),

    /** Text without a signature: markdown, plain text, any encoding. See [looksLikeText]. */
    TEXT("text"),

    /** Nothing but zero bytes: an incomplete download or copy. */
    ZEROS("zeros"),

    /** None of the above and not text: damaged, encrypted, or a format we don't know. */
    BINARY("binary");

    val isImage: Boolean
        get() = this in IMAGES

    val isArchive: Boolean
        get() = this in ARCHIVES

    val isText: Boolean
        get() = this == TEXT || this == HTML || this == XML

    companion object {
        private val IMAGES = setOf(PNG, JPEG, WEBP, TIFF, GIF, BMP, JPEG2000)
        private val ARCHIVES = setOf(ZIP, RAR, SEVEN_ZIP, TAR)
    }
}

/** How much of a file [detectFileContent] looks at. */
const val FILE_CONTENT_SAMPLE_SIZE = 4096

/** The content of the file at [path], or null if it is empty or can't be read. */
fun detectFileContent(path: String): FileContent? {
    val bytes = ByteArray(FILE_CONTENT_SAMPLE_SIZE)
    val read = try {
        File(path).inputStream().use { input ->
            var total = 0
            while (total < bytes.size) {
                val n = input.read(bytes, total, bytes.size - total)
                if (n < 0) break
                total += n
            }
            total
        }
    } catch (e: Exception) {
        return null
    }
    return detectFileContent(bytes, read)
}

/** The content told by the first [size] bytes of a file, or null if there are none. */
fun detectFileContent(bytes: ByteArray, size: Int = bytes.size): FileContent? {
    if (size <= 0) return null
    val b = Sample(bytes, size)
    return when {
        /* mupdf looks for either header anywhere in the first kilobyte and takes the first */
        b.pdfMarker(within = 1024) == 'P' -> FileContent.PDF
        b.pdfMarker(within = 1024) == 'F' -> FileContent.FDF
        b.startsWith("AT&TFORM") -> FileContent.DJVU
        b.startsWith("PK\u0003\u0004") || b.startsWith("PK\u0005\u0006") || b.startsWith("PK\u0007\u0008") -> FileContent.ZIP
        b.startsWith("Rar!\u001A\u0007") -> FileContent.RAR
        b.startsWith(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> FileContent.SEVEN_ZIP
        b.at(257, "ustar") -> FileContent.TAR
        b.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> FileContent.PNG
        b.startsWith(0xFF, 0xD8, 0xFF) -> FileContent.JPEG
        b.startsWith("RIFF") && b.at(8, "WEBP") -> FileContent.WEBP
        b.startsWith("II*\u0000") || b.startsWith("MM\u0000*") || b.startsWith("II+\u0000") || b.startsWith("MM\u0000+") -> FileContent.TIFF
        b.startsWith("GIF87a") || b.startsWith("GIF89a") -> FileContent.GIF
        /* "BM" alone could start a text; the four reserved bytes after the size are zero */
        b.startsWith("BM") && size >= 10 && (6 until 10).all { b[it] == 0 } -> FileContent.BMP
        b.startsWith(0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20) || b.startsWith(0xFF, 0x4F, 0xFF, 0x51) -> FileContent.JPEG2000
        b.at(4, "ftyp") -> FileContent.ISO_MEDIA
        (0 until size).all { b[it] == 0 } -> FileContent.ZEROS
        looksLikeText(bytes, size) -> textKind(b)
        else -> FileContent.BINARY
    }
}

/**
 * Text has no signature, so it's told by what it lacks: a byte order mark settles it, otherwise
 * there must be no zero byte and hardly any control characters. Bytes above 0x7F are fine, so
 * UTF-8 and the legacy 8-bit and multibyte encodings (cp1251, GBK, Shift_JIS) all pass. UTF-16
 * without a mark is the one text this misses; random or encrypted data has a zero byte in the
 * first 4 KB with near certainty, so it isn't taken for text.
 */
internal fun looksLikeText(bytes: ByteArray, size: Int): Boolean {
    val b = Sample(bytes, size)
    if (b.startsWith(0xEF, 0xBB, 0xBF) || b.startsWith(0xFF, 0xFE) || b.startsWith(0xFE, 0xFF)) return true
    var controls = 0
    for (i in 0 until size) {
        val c = b[i]
        if (c == 0) return false
        /* tab, line feed, vertical tab, form feed, carriage return and escape occur in text */
        if ((c < 0x20 && c !in 0x09..0x0D && c != 0x1B) || c == 0x7F) controls++
    }
    return controls * 100 <= size
}

private fun textKind(b: Sample): FileContent {
    val start = b.skipBomAndSpaces()
    return when {
        b.atIgnoreCase(start, "<!doctype html") || b.atIgnoreCase(start, "<html") -> FileContent.HTML
        b.at(start, "<?xml") -> FileContent.XML
        else -> FileContent.TEXT
    }
}

private class Sample(private val bytes: ByteArray, val size: Int) {
    operator fun get(i: Int): Int = bytes[i].toInt() and 0xFF

    fun startsWith(vararg values: Int): Boolean =
        size >= values.size && values.indices.all { this[it] == values[it] }

    fun startsWith(text: String): Boolean = at(0, text)

    fun at(offset: Int, text: String): Boolean =
        size >= offset + text.length && text.indices.all { this[offset + it] == text[it].code }

    fun atIgnoreCase(offset: Int, text: String): Boolean =
        size >= offset + text.length &&
                text.indices.all { this[offset + it].toChar().lowercaseChar() == text[it].lowercaseChar() }

    /** 'P' or 'F' for the first "%PDF-" or "%FDF-" in the first [within] bytes, else null. */
    fun pdfMarker(within: Int): Char? =
        (0..minOf(within, size) - 5).firstNotNullOfOrNull {
            when {
                at(it, "%PDF-") -> 'P'
                at(it, "%FDF-") -> 'F'
                else -> null
            }
        }

    fun skipBomAndSpaces(): Int {
        var i = if (startsWith(0xEF, 0xBB, 0xBF)) 3 else 0
        while (i < size && this[i].toChar().isWhitespace()) i++
        return i
    }
}
