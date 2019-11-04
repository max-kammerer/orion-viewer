package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import androidx.test.rule.ActivityTestRule


abstract class InstrumentationTestCase : TestUtil {

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    @Rule
    @JvmField
    var mActivityRule: ActivityTestRule<OrionViewerActivity> = ActivityTestRule(OrionViewerActivity::class.java, true, false)

    val activity: OrionViewerActivity
        get() = mActivityRule.activity

    override fun getOrionTestContext(): Context = mActivityRule.activity.orionContext

    fun getController() : Controller = mActivityRule.activity.controller!!

    fun startActivityWithBook(intent: Intent) {
        mActivityRule.launchActivity(intent)
        activity.orionContext.isTesting = true
    }
}