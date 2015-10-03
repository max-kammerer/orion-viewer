package universe.constellation.orion.viewer.test

import android.test.ActivityInstrumentationTestCase2
import universe.constellation.orion.viewer.OrionViewerActivity
import android.content.Context
import universe.constellation.orion.viewer.Controller

/**
 * User: mike
 * Date: 21.10.13
 * Time: 19:50
 */

open class InstrumentationTestCase : ActivityInstrumentationTestCase2<OrionViewerActivity>(OrionViewerActivity::class.java), TestUtil {

    override fun getOrionTestContext(): Context {
        return instrumentation!!.context!!
    }

    override fun getActivity(): OrionViewerActivity {
        return super.getActivity()!!
    }

    fun getController() : Controller {
        return activity.controller!!
    }
}