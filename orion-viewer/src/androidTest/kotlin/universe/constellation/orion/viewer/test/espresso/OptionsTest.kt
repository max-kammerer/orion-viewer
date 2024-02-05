package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.framework.BookDescription

@Ignore
@RunWith(Parameterized::class)
class OptionsTest(bookDescription: BookDescription): BaseEspressoTest(bookDescription) {
    @Test
    fun testLongClick() {
        onView(withId(R.id.view)).perform(swipeUp())
        activityScenarioRule.scenario.onActivity {
            Assert.assertEquals(2, it.controller!!.pageLayoutManager.visiblePages.count())
        }
        onView(withId(R.id.view)).perform(longClick())
        device.pressBack()

        activityScenarioRule.scenario.onActivity {
            Assert.assertEquals(2, it.controller!!.pageLayoutManager.visiblePages.count())
        }

        //TODO check pages and their positions
    }
}