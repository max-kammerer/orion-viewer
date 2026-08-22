package universe.constellation.orion.viewer.bookmarks

import android.content.Context
import android.os.Environment
import java.io.File

const val BOOKMARKS_SUFFIX = ".bookmarks.xml"

const val ALL_BOOKMARKS_SUFFIX = ".all_bookmarks.xml"

const val EXPORT_FOLDER = "orion"

private const val DEFAULT_EXPORT_NAME = "orion"

/**
 * Bookmarks are exported next to the book itself, but the book folder is not always writable:
 * the book can be opened from a read only location or from a temporary copy in the app cache.
 * In such cases fallback to the public Documents folder and then to the app external one.
 */
fun Context.bookmarksExportFile(bookPath: String?, suffix: String): File {
    val bookFile = bookPath?.takeIf { it.isNotBlank() }?.let { File(it) }
    val fileName = (bookFile?.name ?: DEFAULT_EXPORT_NAME) + suffix

    val bookFolder = bookFile?.parentFile
    if (bookFolder != null && bookFolder.canWrite() && !isAppCacheFolder(bookFolder)) {
        return File(bookFolder, fileName)
    }

    val documents = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        EXPORT_FOLDER
    )
    if ((documents.exists() || documents.mkdirs()) && documents.canWrite()) {
        return File(documents, fileName)
    }

    return File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir, fileName)
}

fun Context.isAppCacheFolder(folder: File): Boolean {
    val path = folder.absolutePath
    return path.startsWith(cacheDir.absolutePath) ||
            externalCacheDir?.let { path.startsWith(it.absolutePath) } == true
}
