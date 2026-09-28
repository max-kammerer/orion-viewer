package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.test.engine.BrokenImagePageTest
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.view.PageState

/**
 * A page that fails to load is laid out with a stub size and says so, instead of an endless
 * loading indicator (in a debug build the failed layout job used to crash the app), and double
 * tap, which selects a word by default, doesn't reach the engine and crash as it did in 0.97.0.
 */
@RunWith(Parameterized::class)
class BrokenPageViewerTest(book: BookFile) : BaseViewerActivityTest(book) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Broken page in {0}")
        fun testData(): Iterable<BookFile> =
            listOf(BookFile(BrokenImagePageTest.BROKEN_JPEG), BookFile(BrokenImagePageTest.BROKEN_TIFF))
    }

    @Test
    fun brokenPageIsShownAndSurvivesDoubleTap() {
        Espresso.onIdle()
        onActivity {
            val pageView = it.controller!!.pageLayoutManager.activePages.first()
            assertEquals(PageState.SIZE_AND_BITMAP_CREATED, pageView.state)
            assertNotNull("Page should be marked as broken", pageView.page.loadError)
        }

        onView(withId(R.id.view)).perform(doubleClick())
        Espresso.onIdle()

        onActivity {
            assertNotNull(it.controller)
        }
    }
}
