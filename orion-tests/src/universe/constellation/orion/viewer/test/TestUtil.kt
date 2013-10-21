package universe.constellation.orion.viewer.test

import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.FileUtil
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.test.AndroidTestCase
import android.os.Environment

/**
 * User: mike
 * Date: 20.10.13
 * Time: 8:32
 */

trait TestUtil {

    public fun openTestDocument(relativePath: String) : DocumentWrapper {
        val fileOnSdcard = extractFileFromTestData(relativePath)
        return FileUtil.openFile(fileOnSdcard)!!;
    }

    public fun extractFileFromTestData(fileName: String): File {
        val outFile = File(testFolder, fileName)
        if (outFile.exists()) {
            return outFile
        }

        val input =  getTestContext().getAssets()!!.open(getFileUnderTestData(fileName))
        input.buffered().copyTo(FileOutputStream(outFile).buffered())
        return outFile
    }

    public fun getFileUnderTestData(relativePath: String): String {
        return "testData/${relativePath}"
    }

    fun getTestContext(): Context


    public class object {
        public val testFolder: File = File(Environment.getExternalStorageDirectory(), "orion")

        public val SICP: String = "sicp.pdf"

        public val ORION_PKG: String = "universe.constellation.orion.viewer"
    }
}