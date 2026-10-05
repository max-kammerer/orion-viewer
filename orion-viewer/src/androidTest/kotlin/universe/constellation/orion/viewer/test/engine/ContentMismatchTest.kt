package universe.constellation.orion.viewer.test.engine

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.formats.FileFormats
import universe.constellation.orion.viewer.formats.findContentMismatch
import java.io.File
import kotlin.random.Random

/**
 * What a book that failed to open is told to be. The reports that led here: 8 and 21 MB files
 * named .pdf holding random-looking bytes ("no objects found").
 */
class ContentMismatchTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "content_mismatch").apply { mkdirs() }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun file(name: String, bytes: ByteArray) = File(dir, name).apply { writeBytes(bytes) }

    @Test
    fun encryptedDataNamedPdf() {
        val mismatch = findContentMismatch(file("book.pdf", Random(1).nextBytes(64 * 1024).also { it[1] = 0 }))!!
        assertEquals(FileFormats.PDF, mismatch.format)
        assertEquals(FileContent.BINARY, mismatch.content)
        assertEquals("not a pdf file, content=binary", mismatch.toString())
        assertTrue(mismatch.describe(context.resources), mismatch.describe(context.resources).contains("PDF"))
    }

    @Test
    fun otherFormatsUnderBookNames() {
        assertEquals(FileContent.HTML, findContentMismatch(file("page.pdf", "<!DOCTYPE html><html>".toByteArray()))!!.content)
        assertEquals(FileContent.PDF, findContentMismatch(file("pdf_named.djvu", "%PDF-1.4\n".toByteArray()))!!.content)
        assertEquals(FileContent.ZIP, findContentMismatch(file("archive.pdf", byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3)))!!.content)
        assertEquals(FileContent.ZEROS, findContentMismatch(file("download.djvu", ByteArray(8192)))!!.content)
    }

    @Test
    fun expectedContentIsNoMismatch() {
        assertNull(findContentMismatch(file("damaged.pdf", "%PDF-1.7\n1 0 obj <<".toByteArray())))
        assertNull(findContentMismatch(file("notes.md", "# Notes\n\nText".toByteArray())))
        /* any archive opens as a comic book, any image as an image */
        assertNull(findContentMismatch(file("comic.cbz", "Rar!\u001A\u0007\u0000".toByteArray())))
        assertNull(findContentMismatch(file("photo.jpg", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))))
    }

    @Test
    fun unknownExtensionOrEmptyFileIsNotJudged() {
        assertNull(findContentMismatch(file("book.txt", Random(2).nextBytes(1024))))
        assertNull(findContentMismatch(file("empty.pdf", ByteArray(0))))
    }
}
