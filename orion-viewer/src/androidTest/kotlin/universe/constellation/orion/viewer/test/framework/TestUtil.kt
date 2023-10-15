package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.os.Environment
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.document.DocumentWithCaching
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun openTestBook(relativePath: String) : DocumentWithCaching {
    val fileOnSdcard = extractFileFromTestData(relativePath)
    return FileUtil.openFile(fileOnSdcard)
}

fun extractFileFromTestData(fileName: String): File {
    val outFile = File(TestUtil.testFolder, fileName)
    if (outFile.exists()) {
        return outFile
    }
    try {
        outFile.parentFile!!.mkdirs()
        outFile.createNewFile()
    } catch (e: IOException) {
        throw RuntimeException("Couldn't create new file " + outFile.absolutePath, e)
    }

    val input = ClassLoader.getSystemClassLoader().getResourceAsStream(getFileUnderTestData(fileName))
    val bufferedOutputStream = FileOutputStream(outFile).buffered()
    input.buffered().copyTo(bufferedOutputStream)
    bufferedOutputStream.close()
    return outFile
}

fun getFileUnderTestData(relativePath: String): String = "testData/$relativePath"


interface TestUtil {
    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "orion")

        const val SICP: String = "sicp.pdf"

        const val ALICE: String = "aliceinw.djvu"

        const val DJVU_SPEC: String = "DjVu3Spec.djvu"

        const val ORION_PKG: String = "universe.constellation.orion.viewer"
    }
}