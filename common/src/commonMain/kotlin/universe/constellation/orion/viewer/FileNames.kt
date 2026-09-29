package universe.constellation.orion.viewer

import java.security.MessageDigest

/** The file name limit of ext4 and f2fs, and of the FUSE layers over them, in bytes. */
const val MAX_FILE_NAME_BYTES = 255

private const val HASH_CHARS = 16

private const val MAX_EXTENSION_CHARS = 10

/**
 * [name] as a single file name within [maxBytes] of UTF-8. A name that fits is left as is, so
 * files saved under it are still found. A longer one keeps its extension (the engines and the
 * format detection go by it), has its stem cut at a character boundary and gets a hash of the
 * whole name, so long names sharing a beginning stay apart. Each Cyrillic letter takes two
 * bytes, or six once percent-encoded, so a book title easily goes over. '/' can't be in a name
 * and becomes '_'.
 */
fun fitFileName(name: String, maxBytes: Int = MAX_FILE_NAME_BYTES): String {
    val plain = name.replace('/', '_')
    if (plain.utf8Size() <= maxBytes) return plain

    val dot = plain.lastIndexOf('.')
    val extension = if (dot > 0 && plain.length - dot - 1 in 1..MAX_EXTENSION_CHARS) plain.substring(dot) else ""
    val stem = plain.substring(0, plain.length - extension.length)
    val budget = maxBytes - extension.utf8Size() - 1 - HASH_CHARS
    require(budget > 0) { "No room for a file name in $maxBytes bytes" }
    return stem.takeUtf8(budget) + "-" + sha1Hex(name).take(HASH_CHARS) + extension
}

fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

/** The longest prefix within [maxBytes] of UTF-8, never splitting a character. */
private fun String.takeUtf8(maxBytes: Int): String {
    var bytes = 0
    var end = 0
    while (end < length) {
        val codePoint = Character.codePointAt(this, end)
        val size = when {
            codePoint < 0x80 -> 1
            codePoint < 0x800 -> 2
            codePoint < 0x10000 -> 3
            else -> 4
        }
        if (bytes + size > maxBytes) break
        bytes += size
        end += Character.charCount(codePoint)
    }
    return substring(0, end)
}

fun sha1Hex(text: String): String =
    MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
