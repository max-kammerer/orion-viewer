package universe.constellation.orion.viewer.test

import android.test.ActivityInstrumentationTestCase2
import universe.constellation.orion.viewer.OrionViewerActivity
import android.content.Context

/**
 * User: mike
 * Date: 21.10.13
 * Time: 19:50
 */

open class InstrumentationTestCase : ActivityInstrumentationTestCase2<OrionViewerActivity>(javaClass<OrionViewerActivity>()), TestUtil {


    override fun getTestContext(): Context {
        return getInstrumentation()!!.getContext()!!
    }


    override fun getActivity(): OrionViewerActivity {
        return super<ActivityInstrumentationTestCase2>.getActivity()!!
    }
}