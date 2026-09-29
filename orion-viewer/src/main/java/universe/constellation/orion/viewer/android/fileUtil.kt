package universe.constellation.orion.viewer.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.system.Os
import androidx.core.database.getStringOrNull
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.analytics.Analytics
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun getFileInfo(context: Context, uri: Uri, analytics: Analytics): FileInfo? {
    val authority = uri.authority
    val id = uri.lastPathSegment
    val host = uri.host
    val scheme = uri.scheme
    log(
        """ 
            Uri:            
            Authority: $authority
            Fragment: ${uri.fragment}
            Port: ${uri.port}
            Query: ${uri.query}
            Scheme: $scheme
            Host: $host
            Segments: ${uri.pathSegments}
            Id: $id
            """.trimIndent()
    )

    if (ContentResolver.SCHEME_FILE == scheme) {
        return uri.path?.let { path ->
            val file = File(path)
            FileInfo(file.name, file.length(), file.name, path, uri)
        }
    }

    if (ContentResolver.SCHEME_CONTENT != scheme) return null

    val lastModifiedColumn = lastModifiedColumn(context, uri)
    val columns = queryFileColumns(context, uri, FILE_COLUMNS + listOfNotNull(lastModifiedColumn?.first), analytics)
    val displayName = columns[MediaStore.MediaColumns.DISPLAY_NAME]
    val sizeOrZero = columns[MediaStore.MediaColumns.SIZE]?.toLongOrNull() ?: 0
    val dataPath = columns[MediaStore.MediaColumns.DATA]
    val lastModified = lastModifiedColumn?.let { (column, toMillis) ->
        columns[column]?.toLongOrNull()?.takeIf { it > 0 }?.let { it * toMillis }
    }

    dataPath?.let {
        val file = File(it)
        val fileSize = if (file.length() != 0L) file.length() else sizeOrZero
        return FileInfo(displayName, fileSize, id, dataPath, uri, lastModified = lastModified)
    }

    try {
        log("Obtaining path through descriptor via $authority")
        val (pathFromDescriptor, fileLength) = context.contentResolver.openFileDescriptor(uri, "r")?.use { getFileDataFromDescriptor(it) } ?: return null
        if (pathFromDescriptor == null) return null
        return FileInfo(
            displayName ?: pathFromDescriptor.substringAfterLast("/"),
            if (fileLength != 0L) fileLength else sizeOrZero,
            id,
            pathFromDescriptor,
            uri,
            lastModified = lastModified
        ).also {
            log("Returning descriptor file info: $it")
        }
    } catch (e: FileNotFoundException) {
        //the descriptor is only a shortcut to the path, a provider may well refuse it and still serve the data
        return FileInfo(displayName, sizeOrZero, id, "", uri, readError = streamError(context, uri, e, analytics), lastModified = lastModified)
    } catch (e: SecurityException) {
        return FileInfo(displayName, sizeOrZero, id, "", uri, readError = streamError(context, uri, e, analytics), lastModified = lastModified)
    } catch (e: Throwable) {
        errorInDebugOr(e.toString()) { e.printStackTrace() }
    }

    return FileInfo(displayName, sizeOrZero, id, "", uri, lastModified = lastModified)
}

/**
 * Checks the source the way it's going to be read: the stream is what the copy into
 * the temporary file opens. Returns the failure if the data is unreachable that way too.
 */
private fun streamError(context: Context, uri: Uri, descriptorError: Exception, analytics: Analytics): Exception? {
    log("No descriptor for $uri: $descriptorError")
    try {
        context.contentResolver.openInputStream(uri)?.close() ?: return descriptorError
        return null
    } catch (e: FileNotFoundException) {
        //the provider has no such file (any more): a regular outcome, not a bug
        analytics.logWarning("Unreadable source ${uri.authority}: $e")
        return e
    } catch (e: SecurityException) {
        //no grant for the uri, e.g. an expired one-off grant
        analytics.logWarning("Unreadable source ${uri.authority}: $e")
        return e
    }
}


private val FILE_COLUMNS = listOf(
    MediaStore.MediaColumns.DISPLAY_NAME,
    MediaStore.MediaColumns.SIZE,
    MediaStore.MediaColumns.DATA
)

/**
 * The column with the source's modification time and its unit in ms, for the providers known to
 * have one. Asked only there: a provider may reject a projection with a column it doesn't serve,
 * and MediaStore does, which would cost a query per column.
 */
private fun lastModifiedColumn(context: Context, uri: Uri): Pair<String, Long>? = when {
    uri.authority == MediaStore.AUTHORITY -> MediaStore.MediaColumns.DATE_MODIFIED to 1000L
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri) ->
        DocumentsContract.Document.COLUMN_LAST_MODIFIED to 1L
    else -> null
}

/**
 * Name, size and path in one query: each query is a binder call, and for a provider over a
 * network share a trip to the server too. A provider may reject a column it doesn't serve,
 * _data mostly, with IllegalArgumentException; then the columns are asked one by one, so the
 * others are still known. Any other failure means no metadata at all, see [getKeyFromCursor].
 */
private fun queryFileColumns(context: Context, uri: Uri, columns: List<String>, analytics: Analytics): Map<String, String?> {
    try {
        return context.contentResolver.query(uri, columns.toTypedArray(), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return emptyMap()
            columns.associateWith { column ->
                cursor.getColumnIndex(column).takeIf { it >= 0 }?.let { cursor.getStringOrNull(it) }
            }
        } ?: emptyMap()
    } catch (e: IllegalArgumentException) {
        analytics.logWarning("Query of ${uri.authority} rejected the projection: $e")
        return columns.associateWith { getKeyFromCursor(it, context, uri, analytics) }
    } catch (e: SecurityException) {
        analytics.logWarning("SecurityException: ${e.message}")
        return emptyMap()
    } catch (e: RuntimeException) {
        analytics.logWarning("Query of ${uri.authority} failed: $e")
        return emptyMap()
    }
}

private fun getKeyFromCursor(
    column: String,
    context: Context,
    uri: Uri,
    analytics: Analytics
): String? {
    val projection = arrayOf(column)

    try {
        return context.contentResolver.query(uri, projection, null, null, null)?.use {
            if (!it.moveToFirst()) return null
            val columnIndex = it.getColumnIndex(column)
            if (columnIndex < 0) return null
            return it.getStringOrNull(columnIndex)
        }
    } catch (e: SecurityException) {
        analytics.logWarning("SecurityException: ${e.message}")
        return null
    } catch (e: RuntimeException) {
        /* Another app's provider failed in its query(), and its exception came back over binder:
           e.g. NetworkOnMainThreadException from a provider over a network share. The metadata
           is only a hint; the data is still tried through the descriptor or the stream. */
        analytics.logWarning("Query of ${uri.authority} for $column failed: $e")
        return null
    }
}

private fun getFileDataFromDescriptor(pfd: ParcelFileDescriptor): Pair<String?, Long>? {
    return try {
        val file = File("/proc/self/fd/" + pfd.fd)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.readlink(file.absolutePath)
        } else {
            file.canonicalPath
        } to file.length()
    } catch (e: IOException) {
        null
    } catch (e: Exception) {
        null
    }
}

fun FileInfo.isRestrictedAccessPath(): Boolean {
    val path = path
    if (path.startsWith("/data/data")) return true
    if (path.startsWith("/data/obb")) return true
    if (!path.startsWith("/storage/emulated/")) return false
    var suffix = path.substringAfter("/storage/emulated/")
    suffix = suffix.substringAfter("/")
    return suffix.startsWith("Android/data/") || suffix.startsWith("Android/obb/")
}