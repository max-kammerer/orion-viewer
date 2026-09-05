package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.AndroidLogger.log
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseInstrumentationTest
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.doFail
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGeneratedFile
import universe.constellation.orion.viewer.test.framework.onActivityRes
import java.io.File

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class AccessToPrivateFileSecondTimeTest : BaseInstrumentationTest() {

    private val pageCount = 14
    private val fileName = "secondTime.$pageCount.pdf"

    @Test
    fun openViaTemporaryTwice() {
        val firstAttempt =
            launchActivity<OrionViewerActivity>(createContentIntentWithGeneratedFile(fileName))

        log("State: " + firstAttempt.state)
        check(firstAttempt.state == Lifecycle.State.RESUMED || firstAttempt.state == Lifecycle.State.STARTED)

        var time = -1L
        firstAttempt.use {
            /* Click through UiAutomator: right after the previous test's activity closes,
             * the headless CI emulator (API 33+) may leave window focus with the launcher,
             * so Espresso waits 10 s for a focused root and fails. A tap does not need
             * focus and makes the window manager hand it over to the dialog. */
            val text = appContext.getString(R.string.fileopen_open_in_temporary_file)
            (device.wait(Until.findObject(By.text(text)), LONG_TIMEOUT)
                ?: doFail("No fallback dialog")).click()
            it.checkFileWasOpened()
            time = getFileModificationTime(it)
        }

        launchActivity<OrionViewerActivity>(createContentIntentWithGeneratedFile(fileName)).use {
            onView(withId(R.id.view)).check(ViewAssertions.matches(isCompletelyDisplayed()))
            it.checkFileWasOpened()
            val newTime = getFileModificationTime(it)
            Assert.assertEquals(time, newTime)
        }
    }

    private fun getFileModificationTime(it: ActivityScenario<OrionViewerActivity>) =
        it.onActivityRes {
            val file = File(it.controller!!.document.filePath)
            file.lastModified()
        }

    private fun ActivityScenario<OrionViewerActivity>.checkFileWasOpened() {
        checkFileWasOpened(fileName, pageCount)
    }
}