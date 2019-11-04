package universe.constellation.orion.viewer.test.framework

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

/**
 * User: mike
 * Date: 19.10.13
 * Time: 13:45
 */
abstract class BaseTest : TestUtil {

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun getOrionTestContext(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

}