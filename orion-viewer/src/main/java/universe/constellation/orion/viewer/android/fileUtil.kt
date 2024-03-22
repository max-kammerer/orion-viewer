package universe.constellation.orion.viewer.android

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.system.Os
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.log
import java.io.File
import java.io.IOException

fun getPath(context: Context, uri: Uri): FileInfo? {
    log(
        """ 
            Uri:            
            Authority: ${uri.authority}
            Fragment: ${uri.fragment}
            Port: ${uri.port}
            Query: ${uri.query}
            Scheme: ${uri.scheme}
            Host: ${uri.host}
            Segments: ${uri.pathSegments}
            Id: ${uri.lastPathSegment}
            """.trimIndent()
    )

    if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
        checkIsFile(uri.path)?.let { return it }
    }

    if (!ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) return null

    var path: String? = null
    // DocumentProvider
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(
            context,
            uri
        )
    ) {
        log("isDocumentUri")
        // ExternalStorageProvider
        if (FileUtils.isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val type = split[0]
            val externalStorageDirectory = Environment.getExternalStorageDirectory().absolutePath
            if ("primary".equals(
                    type,
                    ignoreCase = true
                ) || externalStorageDirectory.endsWith(type)
            ) {
                path = externalStorageDirectory + "/" + split[1]
            }
            // TODO handle non-primary volumes
        } else if (FileUtils.isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), id.toLong()
            )
            path = getDataColumn(context, contentUri, null, null)
        } else if (FileUtils.isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            when (type) {
                "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val selection = "_id=?"
            val selectionArgs = arrayOf(
                split[1]
            )
            if (contentUri != null) {
                path = getDataColumn(context, contentUri, selection, selectionArgs)
            }
        }
    } else if ("com.google.android.apps.photos.content" == uri.authority) {
        path = uri.lastPathSegment
    }

    checkIsFile(path)?.let {
        return it
    }

    val dataPath = getDataColumn(
        context,
        uri,
        null,
        null
    )
    checkIsFile(dataPath, false)?.let {
        log("data uri")
        return it
    }

    val id = uri.lastPathSegment
    val name = getKeyFromCursor(MediaStore.MediaColumns.DISPLAY_NAME, context, uri)
    val size = getKeyFromCursor(MediaStore.MediaColumns.SIZE, context, uri)

    try {
        log("Obtaining path through descriptor")
        val pathFromDescriptor = context.contentResolver.openFileDescriptor(uri, "r")?.use { getPathFromDescriptor(it) } ?: return null
        return FileInfo(
            name,
            size?.toLong() ?: 0,
            id,
            pathFromDescriptor,
            true
        ).also {
            log("Returning descriptor file info: $it")
        }
    } catch (e: Throwable) {
        errorInDebugOr(e.toString()) { e.printStackTrace() }
    }

    return null
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
        return it.getString(columnIndex)
    }
}

private fun checkIsFile(path: String?, checkIsFile: Boolean = true): FileInfo? {
    if (path == null) return null
    val file = File(path)
    if (!checkIsFile || file.isFile) return FileInfo(file.name, file.length(), file.canonicalPath, path)
    return null
}

/**
 * Copied from koreader
 * tries to get the absolute path of a file from a content provider. It works with most
 * applications that use a fileProvider to deliver files to other applications.
 * If the data in the uri is not a file this will fail
 *
 * @param pfd - parcelable file descriptor from contentResolver.openFileDescriptor
 * @return absolute path to file or null
 */
private fun getPathFromDescriptor(pfd: ParcelFileDescriptor): String? {
    return try {
        val file = File("/proc/self/fd/" + pfd.fd)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.readlink(file.absolutePath)
        } else {
            file.canonicalPath
        }
    } catch (e: IOException) {
        null
    } catch (e: Exception) {
        null
    }
}