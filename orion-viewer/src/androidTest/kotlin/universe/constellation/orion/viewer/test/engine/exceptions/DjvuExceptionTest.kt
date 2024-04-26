package universe.constellation.orion.viewer.test.engine.exceptions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import universe.constellation.orion.viewer.android.isAtLeastLollipop
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import java.io.File

class DjvuExceptionTest : BaseTest() {

    @Test
    fun testFailOnNonExistingFile() {
        try {
            DjvuDocument("1234578910.djvu")
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue(e.message, e.message!!.contains("No such file or directory"))
            return
        }
        fail("Expecting exception to be thrown above")
    }

    @Test
    fun testFailOnEmptyFile() {
        val tmpFIle = File.createTempFile("12345", ".djvu")
        assertTrue("File doesn't exist", tmpFIle.exists())
        assertTrue("Can't read file", tmpFIle.canRead())
        try {
            DjvuDocument(tmpFIle.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAtLeastLollipop()) {
                assertEquals(e.message, "Unexpected End Of File.")
            }
            else {
                assertTrue(e.message, e.message!!.contains("EOF"))
            }
            return
        }
        fail("Expecting exception to be thrown above")
    }


    @Test
    @Ignore
    fun testWrongPage() {
        val doc = DjvuDocument(BookDescription.DJVU_SPEC.asPath())
        try {
            testIncorrectPageNum(doc, 1000)
        } finally {
            doc.destroy()
        }
    }

    private fun testIncorrectPageNum(doc: DjvuDocument, page: Int) {
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