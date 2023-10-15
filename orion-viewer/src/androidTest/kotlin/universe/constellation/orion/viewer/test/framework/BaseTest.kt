package universe.constellation.orion.viewer.test.framework

import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

abstract class BaseTest : TestUtil {

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

}