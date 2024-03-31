package universe.constellation.orion.viewer.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.Os
import androidx.core.database.getStringOrNull
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.log
import java.io.File
import java.io.IOException

fun getFileInfo(context: Context, uri: Uri): FileInfo? {
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

    val displayName = getKeyFromCursor(MediaStore.MediaColumns.DISPLAY_NAME, context, uri)
    val sizeOrZero = getKeyFromCursor(MediaStore.MediaColumns.SIZE, context, uri)?.toLongOrNull() ?: 0

    val dataPath = getDataColumn(
        context,
        uri,
        null,
        null
    )

    dataPath?.let {
        val file = File(it)
        val fileSize = if (file.length() != 0L) file.length() else sizeOrZero
        return FileInfo(displayName, fileSize, id, dataPath, uri)
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
            uri
        ).also {
            log("Returning descriptor file info: $it")
        }
    } catch (e: Throwable) {
        errorInDebugOr(e.toString()) { e.printStackTrace() }
    }

    return FileInfo(displayName, sizeOrZero, id, "", uri)
}


private fun getDataColumn(
    context: Context, uri: Uri, selection: String?,
    selectionArgs: Array<String>?
): String? {
    val column = MediaStore.Files.FileColumns.DATA
    return getKeyFromCursor(column, context, uri, selection, selectionArgs)
}

private fun getKeyFromCursor(
    column: String,
    context: Context,
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): String? {
    val projection = arrayOf(column)

    return context.contentResolver.query(
        uri, projection, selection, selectionArgs,
        null
    )?.use {
        if (!it.moveToFirst()) return null
        val columnIndex = it.getColumnIndex(column)
        if (columnIndex < 0) return null
        return it.getStringOrNull(columnIndex)
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