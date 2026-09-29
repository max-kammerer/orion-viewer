package universe.constellation.orion.viewer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import universe.constellation.orion.viewer.android.isContentUri
import java.io.File
import java.util.Locale

/*
 * Local copies of content:// sources the engines can't read by path, e.g. files on a network
 * share or inside an archive:
 *
 *     cache/content/<authority>/<uri key>/<display name>
 *                                        /.source
 *
 * The uri key is a hash of the uri without its query: the last segment alone is only the file
 * name for path-based providers, and two books called the same would share a copy. The copy
 * keeps the display name, the book parameters are found by it. The `.source` stamp holds the
 * size and modification time of the source and is written once the copy is complete; a copy is
 * reused only while both match. The stamp's own time is the last use, the cleanup goes by it.
 */

/** After each new copy the older ones are removed, least recently used first, down to this. */
const val CONTENT_COPIES_LIMIT_BYTES = 512L shl 20

private const val STAMP = ".source"

fun Context.cacheContentFolder(): File = File(cacheDir, ContentResolver.SCHEME_CONTENT)

private fun Context.copyFolderFor(fileInfo: FileInfo?): File {
    val contentFolder = cacheContentFolder()
    if (fileInfo == null) return contentFolder
    val authority = fitFileName(fileInfo.uri.authority ?: "_")
    return File(File(contentFolder, authority), uriKey(fileInfo.uri))
}

private fun uriKey(uri: Uri): String =
    sha1Hex(uri.buildUpon().clearQuery().fragment(null).build().toString()).take(16)

fun FileInfo.canHaveStableCopy(): Boolean = size != 0L && !name.isNullOrBlank() && uri.isContentUri

private fun FileInfo.stamp(): String = "$size ${lastModified ?: "unknown"}"

/** The copy of [fileInfo] made earlier, if it's complete and the source hasn't changed since. */
fun Context.getValidTmpCopy(fileInfo: FileInfo): File? {
    if (!fileInfo.canHaveStableCopy()) return null
    val folder = copyFolderFor(fileInfo)
    val copy = File(folder, fitFileName(fileInfo.name!!))
    if (!copy.exists() || copy.length() != fileInfo.size) return null
    val stamp = File(folder, STAMP)
    val recorded = try {
        stamp.readText()
    } catch (e: Exception) {
        return null
    }
    if (recorded != fileInfo.stamp()) return null
    stamp.setLastModified(System.currentTimeMillis())
    return copy
}

internal fun Context.createTmpFile(fileInfo: FileInfo?, extension: String): File {
    val fileFolder = copyFolderFor(fileInfo)
    fileFolder.mkdirs()
    if (fileInfo?.canHaveStableCopy() == true) {
        /* The old copy stops being valid until the new one is complete. */
        File(fileFolder, STAMP).delete()
        return File(fileFolder, fitFileName(fileInfo.name!!))
    }
    val fullName = (fileInfo?.name ?: fileInfo?.file?.name ?: "test_book")
    val noExtName = if (fullName.lowercase(Locale.getDefault()).endsWith(".$extension")) {
        fullName.substringBeforeLast(".$extension")
    } else {
        fullName
    }
    /* createTempFile appends up to 19 random digits before the suffix. */
    val prefix = fitFileName(noExtName, MAX_FILE_NAME_BYTES - ".$extension".utf8Size() - 19)
    return File.createTempFile(if (prefix.length < 3) "tmp$prefix" else prefix, ".$extension", fileFolder)
}

/** Called on a worker thread once [copy] of [fileInfo] is complete: stamps it and trims the others. */
internal fun Context.onTmpCopyComplete(copy: File, fileInfo: FileInfo?) {
    val folder = copy.parentFile
    if (fileInfo?.canHaveStableCopy() == true && folder == copyFolderFor(fileInfo)) {
        File(folder, STAMP).writeText(fileInfo.stamp())
    }
    trimContentCopies(keep = folder)
}

/**
 * Removes copies, least recently used first, until all of them take at most [limitBytes]. [keep]
 * (the copy just made, the book about to open) is never removed. A copy is a folder under an
 * authority; loose files and folders of older layouts are removed the same way.
 */
internal fun Context.trimContentCopies(keep: File?, limitBytes: Long = CONTENT_COPIES_LIMIT_BYTES) {
    val entries = cacheContentFolder().listFiles().orEmpty().flatMap { authority ->
        if (authority.isDirectory) authority.listFiles().orEmpty().toList() else listOf(authority)
    }.map { it to it.walkBottomUp().filter { file -> file.isFile }.sumOf { file -> file.length() } }

    var total = entries.sumOf { it.second }
    for ((entry, size) in entries.sortedBy { lastUse(it.first) }) {
        if (total <= limitBytes) break
        if (entry == keep) continue
        log("Removing the copy $entry: ${size shr 10} KB, ${total shr 20} MB in copies")
        if (entry.deleteRecursively()) total -= size
    }
}

private fun lastUse(entry: File): Long {
    File(entry, STAMP).takeIf { it.exists() }?.let { return it.lastModified() }
    return entry.walkBottomUp().maxOfOrNull { it.lastModified() } ?: 0
}
