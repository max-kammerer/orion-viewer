package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.cacheContentFolder
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGeneratedFile
import universe.constellation.orion.viewer.test.framework.onActivity

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class EmptyFileTest :
    BaseTestWithActivity(createContentIntentWithGeneratedFile("empty.0.pdf")) {

    @After
    fun clean() {
        appContext.cacheContentFolder().deleteRecursively()
    }

    @Test
    fun openViaTemporaryFile() {
        onTextNotButtonView(R.string.fileopen_open_in_temporary_file).perform(click())
        onView(withSubstring(getStringRes(R.string.fileopen_file_is_emppty))).check(matches(isDisplayed()))
        onView(withSubstring(getStringRes(R.string.string_close))).perform(click())
        onView(ViewMatchers.withId(R.id.problem_view)).check(matches(isCompletelyDisplayed()))
        onView(withSubstring(getStringRes(R.string.fileopen_file_is_emppty))).perform(click())
        onActivity {
            Assert.assertNull(it.controller)
        }
    }

}