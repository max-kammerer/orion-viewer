package universe.constellation.orion.viewer.test.engine

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.R
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
        assertEquals(FileContent.HTML, findContentMismatch(file("page.djvu", "<!DOCTYPE html><html>".toByteArray()))!!.content)
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
        /* opened by the engine for the content whatever the name (FileUtil.openFile): a failure is a damaged file */
        assertNull(findContentMismatch(file("pdf_named.djvu", "%PDF-1.4\n".toByteArray())))
        assertNull(findContentMismatch(file("djvu_named.pdf", "AT&TFORM\u0000\u0000\u0010\u0000DJVM".toByteArray())))
    }

    /* From the reports of files picked through the system dialog, which lets any file through. */
    @Test
    fun filesOfOtherKindsWithTheirOwnNames() {
        fun check(name: String, bytes: ByteArray, content: FileContent, says: String) {
            val mismatch = findContentMismatch(file(name, bytes))!!
            assertNull(mismatch.format)
            assertEquals(content, mismatch.content)
            assertEquals("not a supported file, content=${content.name.lowercase()}", mismatch.toString())
            val text = mismatch.describe(context.resources)
            assertTrue(text, text.contains(says))
        }
        check("URLLink.acsm", "<fulfillmentToken fulfillmentType=\"buy\" xmlns=\"http://ns.adobe.com/adept\">".toByteArray(), FileContent.ACSM, "Adobe Digital Editions")
        check("page.mht", "From: <Saved by Blink>\r\nMIME-Version: 1.0\r\nContent-Type: multipart/related;\r\n".toByteArray(), FileContent.MHT, "MHT")
        check("book.rar", "Rar!\u001A\u0007\u0000".toByteArray() + ByteArray(64) { 1 }, FileContent.RAR, "RAR")
        check("F60B97B35F7C07C14D335C77690DCE8E3E558E8A", "<!DOCTYPE html><html>".toByteArray(), FileContent.HTML, context.getString(R.string.fileopen_content_web_page))
    }

    /* mupdf opens these by the content: under their own names a failure is a damaged book, not
       an archive or XML; under another book's name they are named. */
    @Test
    fun reflowableBooks() {
        val epub = "PK\u0003\u0004".toByteArray() + ByteArray(26) + "mimetypeapplication/epub+zip".toByteArray()
        assertNull(findContentMismatch(file("book.epub", epub)))
        assertNull(findContentMismatch(file("book.fb2", "<?xml version=\"1.0\"?><FictionBook>".toByteArray())))
        assertNull(findContentMismatch(file("epub_named.djvu", epub)))
        assertEquals(FileContent.EPUB, findContentMismatch(file("epub_named.pdf", epub))!!.content)
    }

    @Test
    fun unknownExtensionOrEmptyFileIsNotJudged() {
        assertNull(findContentMismatch(file("book.txt", Random(2).nextBytes(1024))))
        assertNull(findContentMismatch(file("empty.pdf", ByteArray(0))))
    }
}
