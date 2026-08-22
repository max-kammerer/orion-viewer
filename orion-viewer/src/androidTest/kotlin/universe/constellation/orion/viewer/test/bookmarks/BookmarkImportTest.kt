package universe.constellation.orion.viewer.test.bookmarks

import androidx.test.core.app.ActivityScenario
import com.google.android.material.navigation.NavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.OrionFileSelectorActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.bookmarks.BookNameAndSize
import universe.constellation.orion.viewer.bookmarks.BookmarkExporter
import universe.constellation.orion.viewer.bookmarks.BookmarkImporter
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.appContext
import java.io.File

class BookmarkImportTest : BaseTest() {

    private val accessor by lazy {
        (appContext.applicationContext as OrionApplication).getBookmarkAccessor()
    }

    /**
     * Import reads a stream, so a file authored outside of the app can be taken from any provider.
     */
    @Test
    fun importedBookmarksKeepPagesAndTexts() {
        val stamp = System.currentTimeMillis()
        val source = BookNameAndSize("import-source-$stamp.pdf", 111)
        val target = BookNameAndSize("import-target-$stamp.pdf", 222)

        val sourceId = accessor.insertOrUpdate(source.name, source.size)
        accessor.insertOrUpdateBookmark(sourceId, FIRST_PAGE, "first")
        accessor.insertOrUpdateBookmark(sourceId, LAST_PAGE, "last")

        val exported = File(appContext.cacheDir, "import-test-$stamp.xml")
        try {
            assertTrue(
                "Nothing was exported",
                BookmarkExporter(accessor, exported.absolutePath).export(sourceId)
            )

            BookmarkImporter(accessor, exported.inputStream(), setOf(source), target).doImport()

            val targetId = accessor.selectBookId(target.name, target.size)
            val imported = accessor.selectBookmarks(targetId)
                .filter { it.page != GOTO_PAGE }
                .map { it.page to it.text }
            assertEquals(listOf(FIRST_PAGE to "first", LAST_PAGE to "last"), imported)
        } finally {
            exported.delete()
        }
    }

    /**
     * A bookmark file can live where the built in browser can't reach it, so the system picker
     * should stay available in the import selector.
     */
    @Test
    fun fileSelectorOffersSystemPicker() {
        ActivityScenario.launch(OrionFileSelectorActivity::class.java).use { scenario ->
            scenario.onActivity {
                val navView = it.findViewById<NavigationView>(R.id.nav_view)
                assertTrue(
                    "System file picker should be offered for bookmark import",
                    navView.menu.findItem(R.id.nav_system_select).isVisible
                )
            }
        }
    }

    companion object {
        private const val GOTO_PAGE = -1

        private const val FIRST_PAGE = 0

        private const val LAST_PAGE = 41
    }
}
