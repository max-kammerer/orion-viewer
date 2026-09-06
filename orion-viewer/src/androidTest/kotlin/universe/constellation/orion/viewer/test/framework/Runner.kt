package universe.constellation.orion.viewer.test.framework

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
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

    override fun onStart() {
        /* Grant "All files access" before any test (and, importantly, before
         * ActivityScenarioRule launches an activity: the app shows its own
         * permission dialog instead of opening the book otherwise). A reinstall
         * resets the grant, so single-test IDE runs rely on this. */
        grantManageExternalStorage()
        super.onStart()
    }

    private fun grantManageExternalStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) return
        val packageName = targetContext.packageName
        val fd = uiAutomation.executeShellCommand("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
        val deadline = System.currentTimeMillis() + SHORT_TIMEOUT
        while (!Environment.isExternalStorageManager() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        val res =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).idlingRes as OperationIdlingResource
        IdlingRegistry.getInstance().unregister(res.res)
        super.finish(resultCode, results)
    }
}
