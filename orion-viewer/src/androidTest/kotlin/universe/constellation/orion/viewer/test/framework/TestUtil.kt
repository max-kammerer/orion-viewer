package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.os.Environment
import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * User: mike
 * Date: 20.10.13
 * Time: 8:32
 */

interface TestUtil {

    fun openTestBook(relativePath: String) : DocumentWrapper {
        val fileOnSdcard = extractFileFromTestData(relativePath)
        return FileUtil.openFile(fileOnSdcard);
    }

    fun extractFileFromTestData(fileName: String): File {
        val outFile = File(testFolder, fileName)
        if (outFile.exists()) {
            return outFile
        }
        try {
            outFile.parentFile!!.mkdirs()
            outFile.createNewFile();
        } catch (e: IOException) {
            throw RuntimeException("Couldn't create new file " + outFile.absolutePath, e)
        }

        val input = getOrionTestContext().assets!!.open(getFileUnderTestData(fileName))
        val bufferedOutputStream = FileOutputStream(outFile).buffered()
        input.buffered().copyTo(bufferedOutputStream)
        bufferedOutputStream.close()
        return outFile
    }

    fun getFileUnderTestData(relativePath: String): String {
        return "testData/$relativePath"
    }

    fun getOrionTestContext(): Context

    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "orion")

        val SICP: String = "sicp.pdf"

        val ALICE: String = "aliceinw.djvu"

        val ORION_PKG: String = "universe.constellation.orion.viewer"
    }
}