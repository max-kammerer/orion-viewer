package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.DocumentWithCaching
import universe.constellation.orion.viewer.document.DocumentWithCachingImpl
import universe.constellation.orion.viewer.pdf.PdfDocument
import java.io.File
import java.util.Locale

object FileUtil {

    private fun isDjvuFile(filePathLowCase: String): Boolean {
        return filePathLowCase.endsWith("djvu") || filePathLowCase.endsWith("djv")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun openFile(fileName: String): DocumentWithCaching {
        return DocumentWithCachingImpl(
            openDocWithoutCacheWrapper(fileName)
        )

    }

    fun openDocWithoutCacheWrapper(fileName: String): Document =
        if (isDjvuFile(fileName.lowercase(Locale.getDefault()))) {
            DjvuDocument(fileName)
        } else {
            PdfDocument(fileName)
        }

    @JvmStatic
    @Throws(Exception::class)
    fun openFile(fileName: File): DocumentWithCaching {
        return openFile(fileName.absolutePath)
    }

}
