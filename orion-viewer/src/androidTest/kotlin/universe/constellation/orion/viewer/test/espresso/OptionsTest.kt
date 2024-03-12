package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import org.hamcrest.Matchers.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookFile

@SdkSuppress(minSdkVersion = 21)
@RunWith(Parameterized::class)
class OptionsTest(bookDescription: BookFile): BaseEspressoTest(bookDescription) {
    @Test
    fun testLongClick() {
        onView(withId(R.id.view)).perform(swipeUp())
        var pageCount = -10
        activityScenarioRule.scenario.onActivity {
            pageCount = it.controller!!.pageLayoutManager.visiblePages.count()
            Assert.assertTrue(it.controller!!.pageLayoutManager.visiblePages.count() > 1)
        }
        onView(withId(R.id.view)).perform(longClick())
        device.pressBack()

        activityScenarioRule.scenario.onActivity {
            Assert.assertTrue(pageCount <= it.controller!!.pageLayoutManager.visiblePages.count())
        }

        //TODO check pages and their positions
    }
}