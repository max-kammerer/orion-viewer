package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.AndroidLogger.log
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseInstrumentationTest
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
            onTextNotButtonView(R.string.fileopen_open_in_temporary_file).perform(ViewActions.click())
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