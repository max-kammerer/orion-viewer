package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.SdkSuppress
import org.hamcrest.core.AllOf
import org.hamcrest.core.IsNot.not
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseInstrumentationTest
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGenerated
import universe.constellation.orion.viewer.test.framework.onActivityRes
import java.io.File

@Ignore
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class AccessToPrivateFileSecondTimeTest : BaseInstrumentationTest() {

    private val pageCount = 14
    private val fileName = "secondTime.$pageCount.pdf"

    @Test
    fun openViaTemporaryFile() {
        val firstAttempt =
            launchActivity<OrionViewerActivity>(createContentIntentWithGenerated(fileName))
        var time = -1L
        firstAttempt.use {
            onView(AllOf.allOf(withSubstring("temporary"), not(isAssignableFrom(Button::class.java)))).perform(ViewActions.click())
            it.checkFileWasOpen()
            time = getFileModificationTime(it)
        }

        launchActivity<OrionViewerActivity>(createContentIntentWithGenerated(fileName)).use {
            onView(withId(R.id.view)).check(ViewAssertions.matches(isCompletelyDisplayed()))
            it.checkFileWasOpen()
            val newTime = getFileModificationTime(it)
            Assert.assertEquals(time, newTime)
        }
    }

    private fun getFileModificationTime(it: ActivityScenario<OrionViewerActivity>) =
        it.onActivityRes {
            val file = File(it.controller!!.document.filePath)
            file.lastModified()
        }

    private fun ActivityScenario<OrionViewerActivity>.checkFileWasOpen() {
        onView(withId(R.id.view)).check(ViewAssertions.matches(isCompletelyDisplayed()))
        onActivity {
            Assert.assertNotNull(it.controller)
            Assert.assertEquals(
                fileName,
                it.controller!!.document.filePath.substringAfterLast("/")
            )
            Assert.assertEquals(
                pageCount,
                it.controller!!.document.pageCount
            )
        }
    }
}