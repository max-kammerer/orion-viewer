package universe.constellation.orion.viewer.test.framework

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.document.DocumentWithCaching
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class BaseTest {

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "Download/orion")

        const val SICP: String = "sicp.pdf"

        const val ALICE: String = "aliceinw.djvu"

        const val DJVU_SPEC: String = "DjVu3Spec.djvu"

        const val ORION_PKG: String = BuildConfig.APPLICATION_ID
    }
}

fun openTestBook(relativePath: String): DocumentWithCaching {
    val fileOnSdcard = extractFileFromTestData(relativePath)
    return FileUtil.openFile(fileOnSdcard)
}

fun extractFileFromTestData(fileName: String): File {
    val outFile = File(BaseTest.testFolder, fileName)
    if (outFile.exists()) {
        return outFile
    }
    try {
        outFile.parentFile!!.mkdirs()
        outFile.createNewFile()
    } catch (e: IOException) {
        throw RuntimeException("Couldn't create new file " + outFile.absolutePath, e)
    }
    val input = InstrumentationRegistry.getInstrumentation().context.assets.open(
        getFileUnderTestData(fileName)
    )
    val bufferedOutputStream = FileOutputStream(outFile).buffered()
    input.buffered().copyTo(bufferedOutputStream)
    bufferedOutputStream.close()
    return outFile
}

fun getFileUnderTestData(relativePath: String): String = "testData/$relativePath"