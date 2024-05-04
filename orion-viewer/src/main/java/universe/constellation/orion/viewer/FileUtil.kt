package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.filemanager.fileExtensionLC
import universe.constellation.orion.viewer.formats.FileFormats
import universe.constellation.orion.viewer.pdf.PdfDocument
import java.io.File

object FileUtil {

    private fun isDjvuFile(filePath: String): Boolean {
        return filePath.fileExtensionLC in FileFormats.DJVU.extensions
    }

    @JvmStatic
    @Throws(Exception::class)
    fun openFile(absolutePath: String): Document {
        try {
            return if (isDjvuFile(absolutePath)) {
                DjvuDocument(absolutePath)
            } else {
                PdfDocument(absolutePath)
            }
        } catch (e: Exception) {
            throw RuntimeException("Error during file opening `$absolutePath`: " + e.message, e)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun openFile(file: File): Document {
        return openFile(file.absolutePath)
    }

}
