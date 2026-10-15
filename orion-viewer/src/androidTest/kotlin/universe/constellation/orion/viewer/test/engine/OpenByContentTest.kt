package universe.constellation.orion.viewer.test.engine

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.detectFileContent
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.formats.findContentMismatch
import universe.constellation.orion.viewer.pdf.PdfDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookFile
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The engine is chosen by the content, not the name. The reports that led here: DjVu books from
 * Mail.ru Cloud, named by a hash, and from WhatsApp, saved as "DOC-20260818-WA0001." with an
 * empty extension, went to mupdf; PDFs and images named .djvu went to djvulibre.
 */
class OpenByContentTest : BaseTest() {

    private companion object {
        const val FB2 = """<?xml version="1.0" encoding="utf-8"?><FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0"><description><title-info><book-title>Probe</book-title></title-info></description><body><section><p>Hello FB2 text.</p></section></body></FictionBook>"""
    }

    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "open_by_content").apply { mkdirs() }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun open(book: BookDescription, name: String): Document =
        FileUtil.openFile(book.asFile().copyTo(File(dir, name), overwrite = true))

    private fun check(book: BookDescription, name: String, engine: Class<out Document>) {
        val document = open(book, name)
        try {
            assertTrue("$name opened by ${document.javaClass.simpleName}", engine.isInstance(document))
            assertEquals(book.pageCount, document.pageCount)
        } finally {
            document.destroy()
        }
    }

    @Test
    fun djvuNamedByHash() = check(BookDescription.ALICE, "F60B97B35F7C07C14D335C77690DCE8E3E558E8A", DjvuDocument::class.java)

    @Test
    fun djvuWithEmptyExtension() = check(BookDescription.ALICE, "DOC-20260818-WA0001.", DjvuDocument::class.java)

    @Test
    fun djvuNamedPdf() = check(BookDescription.ALICE, "book.pdf", DjvuDocument::class.java)

    /* Other formats named .djvu, as in the reports of djvulibre's "IFFByteStream not ready" and
       "Illegal chunk id": opened by mupdf. */
    @Test
    fun pdfNamedDjvu() = checkFile("broken/pdf_named.djvu", PdfDocument::class.java)

    @Test
    fun pngNamedDjvu() = checkFile("broken/png_named.djvu", PdfDocument::class.java)

    private fun checkFile(name: String, engine: Class<out Document>) {
        val document = FileUtil.openFile(BookFile(name).asFile())
        try {
            assertTrue("$name opened by ${document.javaClass.simpleName}", engine.isInstance(document))
            assertEquals(1, document.pageCount)
        } finally {
            document.destroy()
        }
    }

    /* mupdf opens any archive with a picture inside as a comic book, by its content: a .zip, or
       a cloud provider's hash name. One without pictures doesn't open and is told to be extracted. */
    @Test
    fun zipWithPicturesOpensAsComic() {
        listOf("comic.zip", "C0FFEE0123456789").forEach { name ->
            val document = FileUtil.openFile(zip(name, "page1.png" to BookFile("broken/png_named.djvu").asFile().readBytes()))
            try {
                assertTrue("$name opened by ${document.javaClass.simpleName}", document is PdfDocument)
                assertEquals(1, document.pageCount)
            } finally {
                document.destroy()
            }
        }
    }

    /* One without pictures (a book packed for sending): opened by the .zip name as a comic of no
       pages, the viewer then says it's an archive to extract; under another name it doesn't
       open and the content check says the same. */
    @Test
    fun zipWithBookInsideIsAnArchiveWithoutPages() {
        val pdf = "book.pdf" to BookFile("broken/pdf_named.djvu").asFile().readBytes()
        val zip = zip("book.zip", pdf)
        val document = FileUtil.openFile(zip)
        try {
            assertEquals(0, document.pageCount)
        } finally {
            document.destroy()
        }
        assertEquals(FileContent.ZIP, detectFileContent(zip.path))

        val unnamed = zip("C0FFEE0123456780", pdf)
        val failed = try {
            FileUtil.openFile(unnamed).destroy()
            false
        } catch (e: Exception) {
            true
        }
        assertTrue("an archive without pictures under a hash name opened", failed)
        assertEquals(FileContent.ZIP, findContentMismatch(unnamed)!!.content)
    }

    /* The reflowable books the app doesn't list yet: mupdf opens them by the content, as books with
       text, not as a comic of their pictures. */
    @Test
    fun epubAndFb2OpenAsBooksUnderAnyName() {
        listOf("book.epub", "ABCDEF0123456789").forEach { checkText(epub(it), FileContent.EPUB, "Hello EPUB text") }
        listOf("book.fb2", "ABCDEF0123456780").forEach {
            checkText(File(dir, it).apply { writeText(FB2) }, FileContent.FB2, "Hello FB2 text")
        }
    }

    private fun checkText(file: File, content: FileContent, text: String) {
        assertEquals(content, detectFileContent(file.path))
        val document = FileUtil.openFile(file)
        try {
            assertTrue("${file.name} opened by ${document.javaClass.simpleName}", document is PdfDocument)
            val page = document.getOrCreatePageAdapter(0)
            val words = page.getPageText()?.lines?.joinToString(" ") { line -> line.joinToString("") }
            page.destroy()
            assertTrue("${file.name}: $words", words?.contains(text) == true)
        } finally {
            document.destroy()
        }
    }

    private fun epub(name: String): File = File(dir, name).also { file ->
        ZipOutputStream(file.outputStream()).use { out ->
            /* the first entry, stored: the media type the format is told by */
            val type = "application/epub+zip".toByteArray()
            out.putNextEntry(ZipEntry("mimetype").apply {
                method = ZipEntry.STORED; size = type.size.toLong(); crc = CRC32().apply { update(type) }.value
            })
            out.write(type)
            out.closeEntry()
            fun add(entry: String, text: String) {
                out.putNextEntry(ZipEntry(entry)); out.write(text.toByteArray()); out.closeEntry()
            }
            add("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            add("content.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Probe</dc:title></metadata><manifest><item id="c1" href="c1.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/></spine></package>""")
            add("c1.xhtml", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><body><h1>Chapter</h1><p>Hello EPUB text.</p></body></html>""")
        }
    }

    private fun zip(name: String, vararg entries: Pair<String, ByteArray>): File =
        File(dir, name).also { file ->
            ZipOutputStream(file.outputStream()).use { out ->
                entries.forEach { (entry, bytes) ->
                    out.putNextEntry(ZipEntry(entry))
                    out.write(bytes)
                    out.closeEntry()
                }
            }
        }

    @Test
    fun pdfNamedByHash() = check(BookDescription.SICP, "A1B2C3D4E5F60718293A4B5C6D7E8F9012345678", PdfDocument::class.java)
}
