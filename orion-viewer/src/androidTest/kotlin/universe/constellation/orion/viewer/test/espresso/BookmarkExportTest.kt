package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.bookmarks.ALL_BOOKMARKS_SUFFIX
import universe.constellation.orion.viewer.bookmarks.BOOKMARKS_SUFFIX
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.checkTrue
import universe.constellation.orion.viewer.test.framework.onActivity
import java.io.File

/**
 * Export puts bookmarks next to the book, so the book should be opened as a real one:
 * a temp test book has no path at all.
 */
@RunWith(Parameterized::class)
class BookmarkExportTest(bookDescription: BookFile) : BaseViewerActivityTest(
    bookDescription,
    bookDescription.toOpenIntent { putExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, false) }
) {

    private val exportedFiles: List<File>
        get() = listOf(BOOKMARKS_SUFFIX, ALL_BOOKMARKS_SUFFIX).map {
            File(bookDescription.asPath() + it)
        }

    @Before
    fun dropPreviousExports() {
        exportedFiles.forEach { it.delete() }
    }

    @After
    fun dropExports() {
        exportedFiles.forEach { it.delete() }
    }

    @Test
    fun exportCurrentBookmarks() {
        exportBookmarksVia(R.string.export_bookmarks_menu_item, BOOKMARKS_SUFFIX)
    }

    @Test
    fun exportAllBookmarks() {
        exportBookmarksVia(R.string.export_all_bookmarks_menu_item, ALL_BOOKMARKS_SUFFIX)
    }

    private fun exportBookmarksVia(menuItemRes: Int, suffix: String) {
        addBookmark()

        activityScenarioRule.scenario.openMenuAndSelect(
            R.id.bookmarks_menu_item,
            R.string.menu_bookmarks_text
        )
        onView(withText(BOOKMARK_TEXT)).check(matches(isDisplayed()))

        selectOptionsMenuItem(menuItemRes)

        val expected = File(bookDescription.asPath() + suffix)
        checkTrue(
            "Bookmarks weren't exported into ${expected.absolutePath}",
            expected.exists() && expected.length() > 0
        )

        device.pressBack()
    }

    private fun addBookmark() {
        onActivity {
            val accessor = it.orionApplication.getBookmarkAccessor()
            val info = it.lastPageInfo!!
            val bookId = accessor.selectBookId(info.simpleFileName, info.fileSize)
                .takeIf { id -> id != -1L }
                ?: accessor.insertOrUpdate(info.simpleFileName, info.fileSize)
            it.orionApplication.tempOptions!!.bookId = bookId
            accessor.insertOrUpdateBookmark(bookId, 1, BOOKMARK_TEXT)
        }
    }

    companion object {
        private const val BOOKMARK_TEXT = "export test bookmark"
    }
}
