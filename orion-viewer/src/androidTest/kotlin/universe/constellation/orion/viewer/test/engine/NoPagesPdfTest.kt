package universe.constellation.orion.viewer.test.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

/**
 * mupdf takes the page count from /Root/Pages/Count as is, so these files open with zero pages,
 * although most of them have one: the viewer reports such a document, and the report has to tell
 * the cases apart. The files live in the broken/ subfolder, away from the folder-scanning suites.
 */
@RunWith(Parameterized::class)
class NoPagesPdfTest(private val file: String, private val expected: List<String>) : BaseTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<Array<Any>> = listOf(
            arrayOf("broken/no_page_count.pdf", listOf("root=/Catalog", "pages=/Pages", "count=missing", "kids=1", "leaves=1")),
            arrayOf("broken/zero_page_count.pdf", listOf("pages=/Pages", "count=0", "kids=1", "leaves=1")),
            arrayOf("broken/no_root.pdf", listOf("root=missing", "leaves=0")),
            /* /Pages points to an object the file doesn't have. */
            arrayOf("broken/missing_pages_node.pdf", listOf("root=/Catalog", "pages=not a dictionary: 9 0 R", "leaves=0", "pageObjects=1", "eof=yes")),
            /* Cut off inside the page tree, as the reports show: no xref, no %%EOF, the page is still there. */
            arrayOf("broken/truncated_pages_node.pdf", listOf("repaired=true", "root=/Catalog", "pages=not a dictionary: 2 0 R", "leaves=0", "pageObjects=1", "eof=no")),
            /* Form data: no pages by design, told apart by the header. */
            arrayOf("broken/form_data.pdf", listOf("header=%FDF-1.2")),
        )
    }

    @Test
    fun zeroPagesAreExplained() {
        val document = BookFile(file).openBook()
        try {
            assertEquals(0, document.pageCount)
            val structure = document.describeStructure()!!
            expected.forEach {
                assertTrue("'$it' missing in: $structure", structure.contains(it))
            }
            assertTrue("The file name leaked into: $structure", !structure.contains(file.substringAfterLast('/')))
        } finally {
            document.destroy()
        }
    }
}
