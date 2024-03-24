package universe.constellation.orion.viewer

import java.io.File

val FileInfo?.sizeOrZero: Long
    get() {
        return this?.size ?: 0
    }

data class FileInfo(val name: String?, val size: Long, val id: String?, val canonicalPath: String, val host: String? = null) {

    val file: File
        get() = File(canonicalPath)
}