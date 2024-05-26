package universe.constellation.orion.viewer.test.espresso

import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.SdkSuppress
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookFile

@RunWith(Parameterized::class)
class AutoCropTest(bookDescription: BookFile) : PageNavigationTest(bookDescription){

    override fun configure() {
        openCropDialog()
        Espresso.onView(ViewMatchers.withText("Auto")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.auto)).perform(ViewActions.click())
        applyCrop()
    }
}