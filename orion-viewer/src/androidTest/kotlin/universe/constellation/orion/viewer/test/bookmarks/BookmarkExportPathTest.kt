package universe.constellation.orion.viewer.test.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import universe.constellation.orion.viewer.bookmarks.BOOKMARKS_SUFFIX
import universe.constellation.orion.viewer.bookmarks.bookmarksExportFile
import universe.constellation.orion.viewer.cacheContentFolder
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.appContext
import java.io.File

/**
 * A book folder is not always a good place for an export: a book opened by content uri is read
 * from a temporary copy in the app cache, and such export would be invisible for a user.
 */
class BookmarkExportPathTest : BaseTest() {

    @Test
    fun exportGoesNextToBook() {
        assumeTrue("Book folder should be writable", testDataFolder.canWrite())
        val book = File(testDataFolder, BOOK_NAME)

        assertEquals(
            File(testDataFolder, BOOK_NAME + BOOKMARKS_SUFFIX),
            appContext.bookmarksExportFile(book.absolutePath, BOOKMARKS_SUFFIX)
        )
    }

    @Test
    fun exportEscapesAppCache() {
        val cachedBook = File(appContext.cacheContentFolder(), "some.provider/1/$BOOK_NAME")

        val target = appContext.bookmarksExportFile(cachedBook.absolutePath, BOOKMARKS_SUFFIX)

        checkIsUsableExportTarget(target)
        assertEquals(BOOK_NAME + BOOKMARKS_SUFFIX, target.name)
    }

    @Test
    fun exportOfBookWithoutPathHasTarget() {
        checkIsUsableExportTarget(appContext.bookmarksExportFile(null, BOOKMARKS_SUFFIX))
    }

    private fun checkIsUsableExportTarget(target: File) {
        assertFalse(
            "Export shouldn't go into the app cache: $target",
            target.absolutePath.startsWith(appContext.cacheDir.absolutePath)
        )
        assertTrue(
            "Export folder should be writable: ${target.parentFile}",
            target.parentFile!!.canWrite()
        )
    }

    companion object {
        private const val BOOK_NAME = "sicp.pdf"
    }
}
