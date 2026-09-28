package universe.constellation.orion.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileNamesTest {

    @Test
    fun nameThatFitsIsKept() {
        assertEquals("sicp.pdf.762.xml", fitFileName("sicp.pdf.762.xml"))
        val exactly = "а".repeat(125) + ".pdf" // 254 bytes
        assertEquals(exactly, fitFileName(exactly))
    }

    @Test
    fun slashIsReplaced() {
        assertEquals("primary:Download_book.pdf", fitFileName("primary:Download/book.pdf"))
    }

    @Test
    fun longNameFitsAndKeepsExtension() {
        val name = "Т".repeat(120) + ".pdf.183066.xml"
        val fitted = fitFileName(name)
        assertTrue(fitted.utf8Size() <= MAX_FILE_NAME_BYTES, "${fitted.utf8Size()} bytes")
        assertTrue(fitted.endsWith(".xml"), fitted)
        assertTrue(fitted.startsWith("ТТТ"), fitted)
        assertEquals(fitted, fitFileName(name), "Not stable")
    }

    @Test
    fun longNamesWithCommonBeginningStayApart() {
        val beginning = "Контроль выполнения ".repeat(10)
        assertNotEquals(fitFileName(beginning + "1.pdf"), fitFileName(beginning + "2.pdf"))
    }

    @Test
    fun charactersAreNotSplit() {
        val name = "😀".repeat(100) + ".djvu"
        val fitted = fitFileName(name)
        assertTrue(fitted.utf8Size() <= MAX_FILE_NAME_BYTES)
        assertTrue(fitted.endsWith(".djvu"))
        /* A cut surrogate pair would not survive the round trip through UTF-8. */
        assertEquals(fitted, String(fitted.toByteArray(Charsets.UTF_8), Charsets.UTF_8))
    }

    @Test
    fun smallerLimit() {
        val fitted = fitFileName("x".repeat(300), maxBytes = 100)
        assertEquals(100, fitted.utf8Size())
    }
}
