package universe.constellation.orion.viewer.formats

import android.content.res.Resources
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.detectFileContent
import universe.constellation.orion.viewer.filemanager.fileExtensionLC
import java.io.File

/**
 * Whether the engine chosen by the file name can open [content]. mupdf picks archive and image
 * decoders by the bytes, so any archive opens as a comic book and any image as an image.
 */
fun FileFormats.accepts(content: FileContent): Boolean = when (this) {
    FileFormats.DJVU -> content == FileContent.DJVU
    /* mupdf opens form data as a document without pages, that case has its own message */
    FileFormats.PDF -> content == FileContent.PDF || content == FileContent.FDF
    FileFormats.XPS -> content == FileContent.ZIP
    FileFormats.CBZ, FileFormats.CBR, FileFormats.CB7, FileFormats.CBT -> content.isArchive
    FileFormats.TIFF, FileFormats.PNG, FileFormats.JPEG, FileFormats.WEBP -> content.isImage
    FileFormats.MD -> content.isText
}

val FileFormats.displayName: String
    get() = when (this) {
        FileFormats.DJVU -> "DjVu"
        FileFormats.MD -> "Markdown"
        FileFormats.WEBP -> "WebP"
        else -> extensions.first().uppercase()
    }

/** A file named as [format] that holds [content], which the engine for [format] can't open. */
class ContentMismatch(val format: FileFormats, val content: FileContent) {

    /** Why the file can't be opened, to follow "Can't open the document: " */
    fun describe(resources: Resources): String = when (content) {
        FileContent.BINARY -> resources.getString(R.string.fileopen_content_unreadable, format.displayName)
        FileContent.ZEROS -> resources.getString(R.string.fileopen_content_zeros)
        else -> resources.getString(
            R.string.fileopen_content_other,
            format.displayName,
            when (content) {
                FileContent.TEXT -> resources.getString(R.string.fileopen_content_text)
                FileContent.HTML -> resources.getString(R.string.fileopen_content_web_page)
                else -> content.shortName
            }
        )
    }

    /** For the problem report: names the case without the file name. */
    override fun toString() = "not a ${format.name.lowercase()} file, content=${content.name.lowercase()}"
}

/**
 * Checks what [file] holds against its name, for a file that failed to open: null when the
 * content is what the engine expects (then the file is a damaged one of its kind) or can't be
 * told, otherwise what was found instead. Reads the first few kilobytes.
 */
fun findContentMismatch(file: File): ContentMismatch? {
    val extension = file.name.fileExtensionLC
    val format = FileFormats.entries.firstOrNull { extension in it.extensions } ?: return null
    val content = detectFileContent(file.path) ?: return null
    return if (format.accepts(content)) null else ContentMismatch(format, content)
}
