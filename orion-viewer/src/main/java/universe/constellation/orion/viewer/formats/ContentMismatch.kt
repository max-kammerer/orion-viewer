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

/**
 * A file named as [format] that holds [content], which the engine for [format] can't open; or,
 * with no [format], a file whose name tells no supported format and whose content no engine opens.
 */
class ContentMismatch(val format: FileFormats?, val content: FileContent) {

    /** Why the file can't be opened, to follow "Can't open the document: " */
    fun describe(resources: Resources): String = when {
        content == FileContent.ACSM -> resources.getString(R.string.fileopen_content_acsm)
        content == FileContent.MHT -> resources.getString(R.string.fileopen_content_mht)
        content == FileContent.ZEROS -> resources.getString(R.string.fileopen_content_zeros)
        format == null && content.isArchive -> resources.getString(R.string.fileopen_content_archive, content.shortName)
        format == null -> resources.getString(R.string.fileopen_content_unsupported, contentName(resources))
        content == FileContent.BINARY -> resources.getString(R.string.fileopen_content_unreadable, format.displayName)
        else -> resources.getString(R.string.fileopen_content_other, format.displayName, contentName(resources))
    }

    private fun contentName(resources: Resources): String = when (content) {
        FileContent.TEXT -> resources.getString(R.string.fileopen_content_text)
        FileContent.HTML -> resources.getString(R.string.fileopen_content_web_page)
        else -> content.shortName
    }

    /** For the problem report: names the case without the file name. */
    override fun toString() =
        "not a ${format?.name?.lowercase() ?: "supported"} file, content=${content.name.lowercase()}"
}

/**
 * Checks what [file] holds against its name, for a file that failed to open: null when the
 * content is what the engine expects (then the file is a damaged one of its kind) or can't be
 * told, otherwise what was found instead. Reads the first few kilobytes.
 */
fun findContentMismatch(file: File): ContentMismatch? {
    val extension = file.name.fileExtensionLC
    val format = FileFormats.entries.firstOrNull { extension in it.extensions }
    val content = detectFileContent(file.path) ?: return null
    /* FileUtil.openFile picks the engine by these contents whatever the name, so the file failed
       in the engine for its kind: a damaged one, not a misnamed one. */
    if (content == FileContent.DJVU) return null
    if (format == FileFormats.DJVU && (content in MUPDF_CONTENTS || content.isArchive || content.isImage)) return null
    /* A name without a supported extension (.acsm, .mht, .rar, a hash): judged only by the
       contents no engine opens; unknown binary data may be any format and isn't named. */
    if (format == null) {
        return if (content in UNOPENED_CONTENTS || content.isArchive) ContentMismatch(null, content) else null
    }
    return if (format.accepts(content)) null else ContentMismatch(format, content)
}

/* What mupdf opens by the content, whatever the name. */
private val MUPDF_CONTENTS = setOf(
    FileContent.PDF, FileContent.FDF, FileContent.EPUB, FileContent.FB2, FileContent.MOBI
)

private val UNOPENED_CONTENTS = setOf(
    FileContent.TEXT, FileContent.HTML, FileContent.XML, FileContent.ACSM, FileContent.MHT,
    FileContent.ISO_MEDIA, FileContent.ZEROS,
    FileContent.OFFICE_OPEN_XML, FileContent.OPEN_DOCUMENT, FileContent.MS_OFFICE,
    FileContent.CHM, FileContent.LIT, FileContent.RTF
)
