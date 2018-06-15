package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.test.ActivityUnitTestCase
import universe.constellation.orion.viewer.OrionViewerActivity

/**
 * User: mike
 * Date: 21.10.13
 * Time: 7:12
 */
open class ActivityBaseTest : ActivityUnitTestCase<OrionViewerActivity>(OrionViewerActivity::class.java), TestUtil {

    override fun setUp() {
        super.setUp()
        val intent = Intent(instrumentation!!.targetContext!!, OrionViewerActivity::class.java)
        intent.data = getDataPath()
        startActivity(intent, null, null)!!.orionContext.isTesting = true
    }

    override fun getOrionTestContext(): Context = instrumentation!!.context!!

    override fun getActivity(): OrionViewerActivity = super.getActivity()!!

    open fun getDataPath() : Uri? = null
}