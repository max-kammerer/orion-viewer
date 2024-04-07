package universe.constellation.orion.viewer.test.framework

import android.app.Application
import android.os.Bundle
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import universe.constellation.orion.viewer.prefs.OrionApplication

class Runner : AndroidJUnitRunner() {

    override fun callApplicationOnCreate(app: Application?) {
        val idleResource = OperationIdlingResource()
        (app as OrionApplication).idlingRes = idleResource
        IdlingRegistry.getInstance().register(idleResource.res)
        super.callApplicationOnCreate(app)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        val res =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).idlingRes as OperationIdlingResource
        IdlingRegistry.getInstance().unregister(res.res)
        super.finish(resultCode, results)
    }
}

