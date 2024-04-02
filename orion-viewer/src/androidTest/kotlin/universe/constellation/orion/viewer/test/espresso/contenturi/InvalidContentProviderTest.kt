package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.SdkSuppress
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.IsNot.not
import org.junit.Ignore
import org.junit.Test
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

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProvider2Test : BaseTestWithActivity(createContentIntentWithGenerated("secondTime.error2.pdf")) {

    @Test
    fun openViaTemporaryFile() {
        onView(
            allOf(withSubstring("temporary"),
                not(ViewMatchers.isAssignableFrom(Button::class.java))
            )
        ).perform(click())

        onView(withSubstring("targetFile")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("GitHub")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("E-Mail")).check(ViewAssertions.matches(isDisplayed()))
    }

}

@Ignore
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class InvalidContentProvider3Test : BaseTestWithActivity(createContentIntentWithGenerated("secondTime.error2.pdf")) {

    @Test
    fun openViaTemporaryFile() {
        onView(
            allOf(withSubstring("Save"),
                not(ViewMatchers.isAssignableFrom(Button::class.java))
            )
        ).perform(click())

        onView(withSubstring("targetFile")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("GitHub")).check(ViewAssertions.matches(isDisplayed()))
        onView(withSubstring("E-Mail")).check(ViewAssertions.matches(isDisplayed()))
    }

}