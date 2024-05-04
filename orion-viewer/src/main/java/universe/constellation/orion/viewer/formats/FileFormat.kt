package universe.constellation.orion.viewer.formats

import android.content.ContentResolver
import android.content.Intent
import android.webkit.MimeTypeMap
import universe.constellation.orion.viewer.filemanager.fileExtension
import universe.constellation.orion.viewer.filemanager.fileExtensionLC
import java.io.File
import java.util.Locale


enum class FileFormats(val extensions: List<String>, vararg val mimeTypes: String) {
    DJVU(
        listOf("djvu", "djv"),
        "image/vnd.djvu",
        "image/x-djvu",
        "image/djvu",
        "application/djvu",
        "application/vnd.djvu"
    ),
    PDF(listOf("pdf"), "application/pdf"),

    XPS(
        listOf("xps", "oxps"),
        "application/vnd.ms-xpsdocument",
        "application/oxps",
        "application/xps"
    ),

    TIFF(listOf("tiff", "tif"), "image/tiff", "image/x-tiff"),
    CBZ(listOf("cbz"), "application/vnd.comicbook+zip", "application/x-cbz"),
    PNG(listOf("png"), "image/png"),
    JPEG(listOf("jpg", "jpeg"), "image/jpeg", "image/pjpeg");

    companion object {
        val supportedMimeTypes by lazy {
            FileFormats.entries.flatMap { it.mimeTypes.toList() }.toTypedArray()
        }

        private val supportedExtensions by lazy {
            FileFormats.entries.flatMap { it.extensions }.toSet()
        }

        private val extToMimeType by lazy {
            entries.associate { it.extensions.first() to it.mimeTypes.first() }
        }

        private val mimeType2Extension by lazy {
            entries.associate { it.mimeTypes.first() to it.extensions.first() }
        }

        fun ContentResolver.getFileExtension(intent: Intent): String? {
            return intent.getFileExtFromPath()?.takeIf { it.isSupportedFileExt }
                ?: mimeType2Extension[getMimeType(intent) ?: return null]
        }

        fun ContentResolver.getMimeType(intent: Intent): String? {
            val type = intent.type
            if (type.isExplicit()) return type

            val uri = intent.data ?: return "<intent/null_data>"
            when (val scheme = intent.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    val mimeTypeFromContent = getType(uri)
                    return mimeTypeFromContent.takeIf { it.isExplicit() } ?: getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(uri.toString()) ?: return "<content/no_extension>"
                    )
                }
                ContentResolver.SCHEME_FILE -> {
                    val file = File(uri.path ?: return  "<file/no_path>")
                    return getMimeTypeFromExtension(file.name.fileExtensionLC.takeIf { it.isNotBlank() } ?: return "<file/no_extension>")
                }
                else -> {
                    return "<unknown_scheme/$scheme>"
                }
            }
        }

        fun Intent.getFileExtFromPath(): String? {
            val uri = data ?: return null
            when (scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    return MimeTypeMap.getFileExtensionFromUrl(uri.toString())

                }
                ContentResolver.SCHEME_FILE -> {
                    val file = File(uri.path ?: return  null)
                    return file.name.fileExtension.takeIf { it.isNotBlank() }
                }
                else -> {
                    return null
                }
            }
        }

        fun String?.isExplicit() = !this.isNullOrBlank() && !contains('*')

        fun getMimeTypeFromExtension(ext: String): String? {
            val extLC = ext.lowercase(Locale.getDefault())
            return FileFormats.extToMimeType[extLC] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extLC)
        }

        val String?.isSupportedFileExt: Boolean
            get() {
                if (this == null) return false
                return supportedExtensions.contains(this.lowercase(Locale.getDefault()))
            }

        val String?.isSupportedMimeType: Boolean
            get() {
                if (this == null) return false
                return mimeType2Extension.containsKey(this)
            }
    }
}

