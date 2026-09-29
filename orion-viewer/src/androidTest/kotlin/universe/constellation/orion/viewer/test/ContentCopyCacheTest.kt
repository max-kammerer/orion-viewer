package universe.constellation.orion.viewer.test

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.cacheContentFolder
import universe.constellation.orion.viewer.createTmpFile
import universe.constellation.orion.viewer.getValidTmpCopy
import universe.constellation.orion.viewer.onTmpCopyComplete
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.trimContentCopies
import java.io.File

/**
 * Copies of content:// sources are keyed by the whole uri and reused only while the source is
 * unchanged; the oldest are removed once the copies take too much. The cache is cleared first:
 * it's the app's own cache, and the cleanup below goes over all of it.
 */
class ContentCopyCacheTest : BaseTest() {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun source(path: String, size: Long = 1000, lastModified: Long? = 1_000_000L) = FileInfo(
        "book.pdf", size, Uri.parse(path).lastPathSegment, "", Uri.parse("content://test.provider/$path"),
        lastModified = lastModified
    )

    private fun copyOf(source: FileInfo): File = context.createTmpFile(source, "pdf").apply {
        writeBytes(ByteArray(source.size.toInt()))
        context.onTmpCopyComplete(this, source)
    }

    private fun File.stamp() = File(parentFile, ".source")

    @Before
    fun clearCopies() {
        context.cacheContentFolder().deleteRecursively()
    }

    @Test
    fun unchangedSourceReusesItsCopy() {
        val copy = copyOf(source("a/book.pdf"))
        assertEquals(copy, context.getValidTmpCopy(source("a/book.pdf")))
    }

    @Test
    fun changedSourceIsNotServedFromTheOldCopy() {
        copyOf(source("a/book.pdf", lastModified = 1_000_000L))
        assertNull(context.getValidTmpCopy(source("a/book.pdf", lastModified = 2_000_000L)))
        assertNull(context.getValidTmpCopy(source("a/book.pdf", size = 999)))
    }

    /* A path-based provider: the last uri segment is only the file name, the same for both. */
    @Test
    fun sameNameInAnotherFolderIsNotMixedUp() {
        copyOf(source("a/book.pdf"))
        assertNull(context.getValidTmpCopy(source("b/book.pdf")))
    }

    @Test
    fun incompleteCopyIsNotReused() {
        context.createTmpFile(source("a/book.pdf"), "pdf").writeBytes(ByteArray(1000))
        assertNull("Never stamped as complete", context.getValidTmpCopy(source("a/book.pdf")))

        copyOf(source("a/book.pdf"))
        context.createTmpFile(source("a/book.pdf"), "pdf")
        assertNull("A new copy has started over the old one", context.getValidTmpCopy(source("a/book.pdf")))
    }

    @Test
    fun withoutModificationTimeTheSizeDecides() {
        val copy = copyOf(source("a/book.pdf", lastModified = null))
        assertEquals(copy, context.getValidTmpCopy(source("a/book.pdf", lastModified = null)))
    }

    @Test
    fun leastRecentlyUsedCopiesAreRemovedFirst() {
        val now = System.currentTimeMillis()
        val (a, b, c) = listOf("a", "b", "c").mapIndexed { i, name ->
            copyOf(source("$name/book.pdf")).also { it.stamp().setLastModified(now - (3 - i) * 60_000L) }
        }
        /* b was opened again: now the most recently used. */
        assertEquals(b, context.getValidTmpCopy(source("b/book.pdf")))

        context.trimContentCopies(keep = null, limitBytes = 2100)
        assertFalse("The least recently used", a.exists())
        assertTrue(b.exists())
        assertTrue(c.exists())
    }

    @Test
    fun keptCopySurvivesTheCleanup() {
        val old = copyOf(source("a/book.pdf")).also { it.stamp().setLastModified(System.currentTimeMillis() - 3_600_000L) }
        val recent = copyOf(source("b/book.pdf"))
        context.trimContentCopies(keep = old.parentFile, limitBytes = 1500)
        assertTrue(old.exists())
        assertFalse(recent.exists())
    }
}
