package universe.constellation.orion.viewer

import android.net.Uri
import java.io.File

data class FileInfo(
    val name: String?,
    val size: Long,
    val id: String?,
    val path: String,
    val uri: Uri
) {

    val file: File by lazy { File(path) }

    val scheme
        get() = uri.scheme!!
}