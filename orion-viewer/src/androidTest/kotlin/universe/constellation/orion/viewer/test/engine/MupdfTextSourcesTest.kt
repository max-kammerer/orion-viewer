package universe.constellation.orion.viewer.test.engine

import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.StructuredText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription

/* PdfPage extracts text from the display list built for rendering rather than from the page,
 * because a display list may be used from any thread while a page may not (mupdf's
 * multi-threading rule 2). This pins down that both sources describe the same text, including
 * character bboxes, for pages without annotations: the list records the very same fz_text
 * objects that the page run hands to the structured-text device. A `showExtras` list adds
 * annotation and widget text on top, which sicp.pdf doesn't have. */
class MupdfTextSourcesTest : BaseTest() {

    @Test
    fun displayListTextMatchesPageText() {
        val document = Document.openDocument(BookDescription.SICP.asPath())
        try {
            var chars = 0
            for (pageNum in PAGES) {
                val page = document.loadPage(pageNum)
                try {
                    val fromPage = page.toStructuredText().use { it.asJSON(1f) }
                    val fromContentList = page.toDisplayList(false).use { it.toStructuredText().use { text -> text.asJSON(1f) } }
                    val fromFullList = page.toDisplayList(true).use { it.toStructuredText().use { text -> text.asJSON(1f) } }
                    assertEquals("Page $pageNum: contents-only display list", fromPage, fromContentList)
                    assertEquals("Page $pageNum: display list with extras", fromPage, fromFullList)
                    chars += page.toStructuredText().use { it.asText().length }
                } finally {
                    page.destroy()
                }
            }
            assertTrue("The sample pages should carry text, got $chars chars", chars > 10_000)
        } finally {
            document.destroy()
        }
    }

    private inline fun <T> StructuredText.use(body: (StructuredText) -> T): T =
        try { body(this) } finally { destroy() }

    private inline fun <T> com.artifex.mupdf.fitz.DisplayList.use(body: (com.artifex.mupdf.fitz.DisplayList) -> T): T =
        try { body(this) } finally { destroy() }

    companion object {
        /* Front matter, a few chapter pages and some deep ones: text, formulas and figures. */
        private val PAGES = listOf(1, 2, 5, 11, 12, 30, 100, 250, 400, 600)
    }
}
