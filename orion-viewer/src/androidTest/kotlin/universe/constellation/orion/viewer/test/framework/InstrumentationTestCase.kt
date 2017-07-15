package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.test.ActivityInstrumentationTestCase2
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity

/**
 * User: mike
 * Date: 21.10.13
 * Time: 19:50
 */

open class InstrumentationTestCase : ActivityInstrumentationTestCase2<OrionViewerActivity>(OrionViewerActivity::class.java), TestUtil {

    override fun getOrionTestContext(): Context = instrumentation!!.context!!

    fun getController() : Controller = activity.controller!!

    fun startActivityWithBook(book: String) {
        val file = extractFileFromTestData(book)
        val intent = Intent()
        intent.data = Uri.fromFile(file)
        setActivityIntent(intent)
    }
}