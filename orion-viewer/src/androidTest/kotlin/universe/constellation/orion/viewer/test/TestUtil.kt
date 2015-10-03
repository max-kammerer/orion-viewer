package universe.constellation.orion.viewer.test

import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.FileUtil
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.test.AndroidTestCase
import android.os.Environment
import java.io.IOException

/**
 * User: mike
 * Date: 20.10.13
 * Time: 8:32
 */

interface TestUtil {

    public fun openTestBook(relativePath: String) : DocumentWrapper {
        val fileOnSdcard = extractFileFromTestData(relativePath)
        return FileUtil.openFile(fileOnSdcard)!!;
    }

    public fun extractFileFromTestData(fileName: String): File {
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

    public fun getFileUnderTestData(relativePath: String): String {
        return "testData/$relativePath"
    }

    fun getOrionTestContext(): Context

    public companion object {
        public val testFolder: File = File(Environment.getExternalStorageDirectory(), "orion")

        public val SICP: String = "sicp.pdf"

        public val ALICE: String = "aliceinw.djvu"

        public val ORION_PKG: String = "universe.constellation.orion.viewer"
    }
}