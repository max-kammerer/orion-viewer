package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.SdkSuppress
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGenerated

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProviderTest : BaseTestWithActivity(createContentIntentWithGenerated("secondTime.error.pdf")) {

    @Test
    fun openViaTemporaryFile() {
        //onView(withSubstring("Error on intent processing")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("GitHub")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("E-Mail")).check(ViewAssertions.matches(isDisplayed()))
    }

}

@Ignore
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProvider2Test : BaseTestWithActivity(createContentIntentWithGenerated("secondTime.error2.pdf")) {

    @Test
    fun openViaTemporaryFile() {
        onTextNotButtonView(R.string.fileopen_save_to_file).perform(click())

        onView(withSubstring("targetFile")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("GitHub")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("E-Mail")).check(ViewAssertions.matches(isDisplayed()))
    }
}