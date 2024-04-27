package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.pdf.PdfDocument
import java.io.File
import java.lang.RuntimeException
import java.util.Locale

object FileUtil {

    private fun isDjvuFile(filePathLowCase: String): Boolean {
        return filePathLowCase.endsWith("djvu") || filePathLowCase.endsWith("djv")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun openFile(absolutePath: String): Document {
        try {
            return if (isDjvuFile(absolutePath.lowercase(Locale.getDefault()))) {
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
