package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.test.AndroidTestCase
import universe.constellation.orion.viewer.device.AndroidDevice

/**
 * User: mike
 * Date: 19.10.13
 * Time: 13:45
 */
abstract class BaseTest : AndroidTestCase(), TestUtil {

    public val device: AndroidDevice = AndroidDevice()

    override fun getOrionTestContext(): Context {
        val m = AndroidTestCase::class.java.getMethod("getTestContext")
        val testContext = m.invoke(this) as Context
        return testContext
    }

}