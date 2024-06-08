package universe.constellation.orion.viewer.test.espresso

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.checkTrue
import universe.constellation.orion.viewer.test.framework.onActivity

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
@RunWith(Parameterized::class)
class LongTapActionTest(bookDescription: BookFile): BaseViewerActivityTest(bookDescription, bookDescription.toOpenIntent {
    this.putExtra(GlobalOptions.Companion.LONG_TAP_ACTION, appContext.getString(R.string.action_key_tap_action))
}) {
    @Test
    fun testLongClick() {
        onView(withId(R.id.view)).perform(swipeUp())
        var visiblePagesOnOpen = -10
        onActivity {
            val count = it.controller!!.pageLayoutManager.visiblePages.count()
            visiblePagesOnOpen = count
            Assert.assertTrue("$count should be > 1",count > 1)
        }
        onView(withId(R.id.view)).perform(longClick())
        device.pressBack()

        val visiblePages = onActivity {
             it.controller!!.pageLayoutManager.visiblePages.count()
        }
        Thread.sleep(1000)
        checkTrue("$visiblePagesOnOpen should be <= $visiblePages", visiblePagesOnOpen <= visiblePages)
        //TODO check pages and their positions
    }
}