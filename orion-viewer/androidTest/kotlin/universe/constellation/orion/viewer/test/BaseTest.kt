package universe.constellation.orion.viewer.test

import android.test.AndroidTestCase
import android.content.res.AssetManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.content.Context
import java.io.File
import android.os.Environment
import java.io.FileOutputStream
import java.io.FileWriter
import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.device.AndroidDevice

/**
 * User: mike
 * Date: 19.10.13
 * Time: 13:45
 */
open class BaseTest : AndroidTestCase(), TestUtil {

    public val device: AndroidDevice = AndroidDevice()

    override fun getTestContext(): Context {
        val m = javaClass<AndroidTestCase>().getMethod("getTestContext")
        val testContext = m.invoke(this) as Context
        return testContext
    }

}