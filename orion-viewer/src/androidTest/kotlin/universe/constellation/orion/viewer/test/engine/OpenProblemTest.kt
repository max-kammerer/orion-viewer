package universe.constellation.orion.viewer.test.engine

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.EngineLibraryMissingException
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.formats.OpenProblem
import java.io.File

/** What the viewer tells and reports for a book it can't show. */
class OpenProblemTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.resources
    private val dir = File(context.cacheDir, "open_problem").apply { mkdirs() }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun file(name: String, bytes: ByteArray) = File(dir, name).apply { writeBytes(bytes) }

    @Test
    fun anotherFormatIsAWarningOfItsOwn() {
        val error = RuntimeException("no objects found")
        val problem = OpenProblem.ofOpenError(file("page.pdf", "<!DOCTYPE html><html>".toByteArray()), error)
        assertTrue(problem.toString(), problem is OpenProblem.Mismatch)
        assertEquals("Warning: another format, name=pdf, content=html", problem.report.message)
        assertSame(error, problem.report.cause)
        assertSame(error, problem.error)
        assertTrue(problem.describe(resources).contains(context.getString(R.string.fileopen_content_web_page)))
    }

    @Test
    fun damagedBookIsTheEngineError() {
        val error = RuntimeException("cannot recognize xref format")
        val problem = OpenProblem.ofOpenError(file("damaged.pdf", "%PDF-1.7\n1 0 obj <<".toByteArray()), error)
        assertTrue(problem.toString(), problem is OpenProblem.EngineError)
        assertSame(error, problem.report)
        assertEquals(resources.getString(R.string.crash_on_book_opening_message_header_panel, "damaged.pdf"), problem.describe(resources))
    }

    @Test
    fun missingEngineTellsTheAbi() {
        val error = EngineLibraryMissingException("armeabi", UnsatisfiedLinkError("no libmupdf_java"))
        val problem = OpenProblem.ofOpenError(file("book.pdf", "%PDF-1.7".toByteArray()), error)
        assertTrue(problem.toString(), problem is OpenProblem.NoEngine)
        assertSame(error, problem.report)
        assertTrue(problem.describe(resources).contains("armeabi"))
    }

    @Test
    fun emptyFile() {
        val problem = OpenProblem.Empty("age=1s")
        assertEquals("Warning: empty file, age=1s", problem.report.message)
        assertEquals(null, problem.error)
        assertTrue(problem.describe(resources).contains(context.getString(R.string.fileopen_file_is_emppty)))
    }

    @Test
    fun archiveWithoutPicturesIsNamed() {
        val book = file("book.zip", byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        val problem = OpenProblem.NoPages(FileContent.ZIP, book, "header=PK..", "media")
        assertEquals("Warning: archive without pages, host=media, size=4, header=PK..", problem.report.message)
        assertTrue(problem.describe(resources).contains("ZIP"))
        assertTrue(OpenProblem.NoPages(null, book, null, null).describe(resources)
            .contains(context.getString(R.string.fileopen_no_pages)))
    }

    /* A report: 131072 bytes of a PDF without the end, the page tree in the lost part. */
    @Test
    fun pdfWithoutItsEndIsCalledIncomplete() {
        val incomplete = context.getString(R.string.fileopen_incomplete_file)
        val cut = file("cut.pdf", "%PDF-1.6\n1 0 obj\n<</Type/Catalog/Pages 642 0 R>>\nendobj\n".toByteArray())
        val cutProblem = OpenProblem.NoPages(FileContent.PDF, cut, null, "media")
        assertTrue(cutProblem.describe(resources).contains(incomplete))
        assertEquals("Warning: incomplete file, host=media, size=${cut.length()}", cutProblem.report.message)

        val whole = file("whole.pdf", "%PDF-1.6\n1 0 obj\n<<>>\nendobj\n%%EOF\n".toByteArray())
        val wholeProblem = OpenProblem.NoPages(FileContent.PDF, whole, null, "media")
        assertTrue(wholeProblem.describe(resources).contains(context.getString(R.string.fileopen_no_pages)))
        assertEquals("Warning: no pages in doc, host=media, size=${whole.length()}", wholeProblem.report.message)
    }
}
