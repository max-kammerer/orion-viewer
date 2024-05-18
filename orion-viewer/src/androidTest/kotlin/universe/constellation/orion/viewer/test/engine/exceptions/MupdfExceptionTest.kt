package universe.constellation.orion.viewer.test.engine.exceptions

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.pdf.PdfDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookFile
import java.io.File

class MupdfExceptionTest : BaseTest() {

    @Test
    fun testFailOnNonExistingFile() {
        try {
            PdfDocument("1234578910.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue(e.message, e.message!!.contains("No such file or directory"))
            return
        }
        fail("Expecting exception to be thrown above")
    }

    @Test
    fun testFailOnEmptyFile() {
        val tmpFIle = File.createTempFile("mupdf12345", ".pdf")
        assertTrue("File doesn't exist", tmpFIle.exists())
        assertTrue("Can't read file", tmpFIle.canRead())
        try {
            PdfDocument(tmpFIle.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue(e.message, e.message!!.contains("no objects found"))
            return
        }
        fail("Expecting exception to be thrown above")
    }

    @Test
    fun testWrongPage() {
        val doc = PdfDocument(BookDescription.SICP.asPath())
        try {
            testIncorrectPageNum(doc, 1000)
        } finally {
            doc.destroy()
        }
    }

    private fun testIncorrectPageNum(doc: PdfDocument, page: Int) {
        try {
            doc.createPage(page).getPageSize()
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue(e.message, e.message!!.contains("page number out of range: 1000 of 762"))
            return
        }
        fail("Expecting exception to be thrown above")
    }
}