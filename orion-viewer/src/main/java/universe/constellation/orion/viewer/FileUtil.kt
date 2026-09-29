package universe.constellation.orion.viewer

import android.os.Build
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.NonFatalErrorReporter
import universe.constellation.orion.viewer.filemanager.fileExtensionLC
import universe.constellation.orion.viewer.formats.FileFormats
import universe.constellation.orion.viewer.pdf.PdfDocument
import java.io.File

/** No native engine library for the device in the installed apk; [abis] is what the device runs. */
class EngineLibraryMissingException(val abis: String, cause: LinkageError) :
    Exception("Engine library isn't available for this device (abi: $abis): " + cause.message, cause)

object FileUtil {

    /** Cache limit for djvu documents opened here; the application sizes it from the device memory. */
    @JvmStatic
    var djvuCacheLimit: Long = DjvuDocument.DEFAULT_CACHE_LIMIT

    private fun isDjvuFile(filePath: String): Boolean {
        return filePath.fileExtensionLC in FileFormats.DJVU.extensions
    }

    /** [errorReporter] gets the problems the document survives, see [NonFatalErrorReporter]. */
    @JvmStatic
    @JvmOverloads
    @Throws(Exception::class)
    fun openFile(file: File, errorReporter: NonFatalErrorReporter = NonFatalErrorReporter.NONE): Document {
        val absolutePath = file.absolutePath
        try {
            return if (isDjvuFile(file.name)) {
                DjvuDocument(absolutePath, djvuCacheLimit, errorReporter)
            } else {
                PdfDocument(absolutePath, errorReporter)
            }
        } catch (e: LinkageError) {
            //the engine library is loaded in a static initializer: an apk without a library for
            //the device abi (a wrong split installed by hand, an armeabi or mips device) fails
            //here with an Error, which the callers' handlers of Exception would let crash the app
            throw EngineLibraryMissingException(deviceAbis(), e)
        } catch (e: EngineLibraryMissingException) {
            throw e
        } catch (e: Exception) {
            /* The header tells what the engine got: DjVu errors like "Illegal chunk id" usually
               mean another format (a PDF, an image, a web page) under a .djvu name. */
            throw RuntimeException(
                "Error during file opening `${file.name}`: " + e.message + "\n" +
                        "(File size: ${file.beautifiedFileSize()}, file path: ${absolutePath}, ${describeFileHeader(absolutePath)})", e
            )
        }
    }

    private fun deviceAbis(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.joinToString()
        } else {
            @Suppress("DEPRECATION")
            listOf(Build.CPU_ABI, Build.CPU_ABI2).filter { !it.isNullOrEmpty() }.joinToString()
        }

    private fun File.beautifiedFileSize(): String {
        return length().beautifyFileSize()
    }

    @JvmStatic
    fun Long.beautifyFileSize(): String {
        if (this < 1024) {
            return "$this bytes"
        }

        var size = this / 1024.0
        if (size < 1024) {
            return String.format("%.2f KB", size)
        }

        size /= 1024.0
        if (size < 1024) {
            return String.format("%.2f MB", size)
        }

        size /= 1024.0
        return String.format("%.2f GB", size)
    }

}
