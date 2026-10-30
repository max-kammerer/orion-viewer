package universe.constellation.orion.viewer.test

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile
import java.io.File

/** The option offering the app for zip and rar files: the system's choice follows it, issue #234. */
class ArchivesAsComicsTest : BaseTest() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val options = (context.applicationContext as OrionApplication).options
    private val dir = File(context.cacheDir, "archives_as_comics").apply { mkdirs() }

    private fun setOption(enabled: Boolean) {
        //on the main thread the listener of the options applies it right in commit()
        instrumentation.runOnMainSync {
            options.prefs.edit().putBoolean("OPEN_ARCHIVES_AS_COMICS", enabled).commit()
        }
    }

    @After
    fun cleanUp() {
        instrumentation.runOnMainSync {
            options.prefs.edit().remove("OPEN_ARCHIVES_AS_COMICS").commit()
        }
        dir.deleteRecursively()
    }

    private fun isOffered(mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse("content://some.provider/1"), mimeType)
            .setPackage(context.packageName)
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    @Test
    fun systemOffersTheAppOnlyWithTheOption() {
        setOption(true)
        assertTrue(isOffered("application/zip"))
        assertTrue(isOffered("application/vnd.rar"))

        setOption(false)
        assertFalse(isOffered("application/zip"))
        assertFalse(isOffered("application/vnd.rar"))
    }

    @Test
    fun fileManagerListsArchivesOnlyWithTheOption() {
        val filter = FileChooserAdapter.DEFAULT_FILTER

        setOption(false)
        assertFalse(filter.accept(dir, "comic.zip"))
        assertFalse(filter.accept(dir, "comic.rar"))

        setOption(true)
        assertTrue(filter.accept(dir, "comic.ZIP"))
        assertTrue(filter.accept(dir, "comic.rar"))
        assertFalse(filter.accept(dir, "comic.7z"))
    }

    @Test
    fun comicNamedZipOpens() {
        val zip = BookFile("webp/comic_webp.cbz").asFile().copyTo(File(dir, "comic.zip"))
        val document = FileUtil.openFile(zip)
        try {
            assertEquals(3, document.pageCount)
        } finally {
            document.destroy()
        }
    }
}
