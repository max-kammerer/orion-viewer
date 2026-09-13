package universe.constellation.orion.viewer

import android.net.Uri
import java.io.File

data class FileInfo(
    val name: String?,
    val size: Long,
    val id: String?,
    val path: String,
    val uri: Uri,
    /** Set when the content provider refused to open the uri: the file can't be read at all. */
    val readError: Exception? = null
) {

    val file: File by lazy { File(path) }

    val scheme
        get() = uri.scheme!!
}