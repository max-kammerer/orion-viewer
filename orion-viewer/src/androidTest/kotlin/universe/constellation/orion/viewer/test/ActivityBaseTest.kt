package universe.constellation.orion.viewer.test

import android.test.ActivityUnitTestCase
import universe.constellation.orion.viewer.OrionViewerActivity
import android.content.Intent
import android.content.Context
import android.net.Uri

/**
 * User: mike
 * Date: 21.10.13
 * Time: 7:12
 */
open class ActivityBaseTest : ActivityUnitTestCase<OrionViewerActivity>(OrionViewerActivity::class.java), TestUtil {

    override fun setUp() {
        super.setUp()
        val intent = Intent(instrumentation!!.targetContext!!, OrionViewerActivity::class.java);
        intent.setData(getDataPath())
        startActivity(intent, null, null)!!.orionContext!!.isTesting = true
    }

    override fun getOrionTestContext(): Context {
        return instrumentation!!.context!!;
    }

    override fun getActivity(): OrionViewerActivity {
        return super.getActivity()!!
    }

    open fun getDataPath() : Uri?  {
        return null;
    }
}