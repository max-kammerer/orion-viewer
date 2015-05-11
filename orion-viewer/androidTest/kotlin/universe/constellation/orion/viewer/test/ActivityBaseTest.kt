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
open class ActivityBaseTest : ActivityUnitTestCase<OrionViewerActivity>(javaClass<OrionViewerActivity>()), TestUtil {

    override fun setUp() {
        super<ActivityUnitTestCase>.setUp()
        val intent = Intent(getInstrumentation()!!.getTargetContext()!!, javaClass<OrionViewerActivity>());
        intent.setData(getDataPath())
        startActivity(intent, null, null)!!.getOrionContext()!!.isTesting = true
    }

    override fun getOrionTestContext(): Context {
        return getInstrumentation()!!.getContext()!!;
    }

    override fun getActivity(): OrionViewerActivity {
        return super<ActivityUnitTestCase>.getActivity()!!
    }

    open fun getDataPath() : Uri?  {
        return null;
    }
}