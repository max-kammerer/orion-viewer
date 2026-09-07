package universe.constellation.orion.viewer.test.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.LinkTarget
import universe.constellation.orion.viewer.document.PageLink
import universe.constellation.orion.viewer.document.findAt
import universe.constellation.orion.viewer.document.toDocPlace
import universe.constellation.orion.viewer.document.withPage
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookFile

/* The expectations for sicp.pdf come from `mutool run` over the same file. */
class LinksTest : BaseTest() {

    private val documents = mutableListOf<Document>()

    @After
    fun closeDocuments() {
        documents.forEach { it.destroy() }
        documents.clear()
    }

    private fun open(book: BookFile): Document = book.openBook().also { documents.add(it) }

    private fun Document.links(page0: Int): List<PageLink> = withPage(page0) { getLinks() }

    @Test
    fun pdfTableOfContentsLinksPointInsideTheBook() {
        val document = open(BookDescription.SICP)
        val links = document.links(2)
        assertEquals(15, links.size)

        val pageSize = document.withPage(2) { getPageSize() }
        links.forEach {
            assertTrue("Link should be inside the page: $it", it.left >= 0 && it.top >= 0)
            assertTrue("Link should be inside the page: $it", it.right <= pageSize.width && it.bottom <= pageSize.height)
            assertTrue("Link should have an area: $it", it.right > it.left && it.bottom > it.top)
            assertTrue("Contents links are internal: $it", it.target is LinkTarget.Internal)
        }

        val first = links.first().target as LinkTarget.Internal
        assertEquals(8, first.page)
        assertEquals(61.2f, first.y, 0.5f)
    }

    @Test
    fun pdfExternalLinksKeepTheirUri() {
        val links = open(BookDescription.SICP).links(1)
        assertEquals(4, links.size)
        val uris = links.map { (it.target as LinkTarget.External).uri }
        assertEquals("http://creativecommons.org/licenses/by-sa/3.0/", uris[0])
        assertEquals("http://www.neilvandyke.org/sicp-texi/", uris[3])
        assertTrue(uris.all { it.startsWith("http://") })
    }

    @Test
    fun pdfPageWithoutLinks() {
        assertTrue(open(BookDescription.SICP).links(0).isEmpty())
    }

    @Test
    fun linksAreLoadedOnce() {
        val document = open(BookDescription.SICP)
        val page = document.getOrCreatePageAdapter(2)
        try {
            assertTrue(page.getLinks() === page.getLinks())
        } finally {
            page.destroy()
        }
    }

    @Test
    fun hitTestFindsTheLinkUnderThePoint() {
        val links = open(BookDescription.SICP).links(2)
        val link = links.first()
        val found = links.findAt((link.left + link.right) / 2, (link.top + link.bottom) / 2)
        assertTrue(found === link)
        assertNull(links.findAt(10f, 10f))
    }

    @Test
    fun destinationBecomesPlaceAtTheTopOfTheScreen() {
        val place = LinkTarget.Internal(8, 64.8f, 442.8f).toDocPlace(PageSize(662, 885))
        assertEquals(8, place.page)
        assertEquals(0f, place.xFraction, 0.001f)
        assertEquals(0.5f, place.yFraction, 0.001f)

        val wholePage = LinkTarget.Internal(3).toDocPlace(PageSize(662, 885))
        assertEquals(0f, wholePage.yFraction, 0.001f)
    }

    @Test
    fun djvuWithoutAnnotationsHasNoLinks() {
        assertTrue(open(BookDescription.ALICE).links(0).isEmpty())
    }

    /* links.djvu: a generated 1240x1754 page (see the visible captions) with an ANTa chunk of five
     * maparea entries: a rect around "Open Orion Viewer on GitHub", an oval around "Jump to this
     * page (#1)", a page number out of range (clamped to the last page, as djview does), an
     * unknown page name and an entry without an area (both dropped).
     * djvu coordinates have the y axis going up, the viewer expects the top-left origin. */
    @Test
    fun djvuMapAreasBecomeLinks() {
        val document = open(BookFile(LINKS_DJVU))
        assertEquals(1, document.pageCount)
        val links = document.links(0)
        assertEquals("Unresolvable and area-less entries are dropped: $links", 3, links.size)

        val external = links[0]
        assertEquals(LinkTarget.External("https://github.com/max-kammerer/orion-viewer"), external.target)
        assertEquals(150f, external.left, 0.01f)
        assertEquals(500f, external.top, 0.01f)
        assertEquals(900f, external.right, 0.01f)
        assertEquals(560f, external.bottom, 0.01f)

        val samePage = links[1]
        assertEquals(LinkTarget.Internal(0), samePage.target)
        assertEquals(150f, samePage.left, 0.01f)
        assertEquals(700f, samePage.top, 0.01f)
        assertEquals(900f, samePage.right, 0.01f)
        assertEquals(760f, samePage.bottom, 0.01f)

        val clamped = links[2]
        assertEquals(LinkTarget.Internal(0), clamped.target)
        assertEquals(150f, clamped.left, 0.01f)
        assertEquals(1104f, clamped.top, 0.01f)

        assertTrue(links.findAt(500f, 530f) === external)
        assertTrue(links.findAt(500f, 730f) === samePage)
        assertNull(links.findAt(500f, 1000f))
    }

    /* The same page with the INFO orientation flag set to 90 degrees: it is rendered rotated
     * (1754x1240), so the areas must follow. Expected values come from GRectMapper::rotate(1)
     * over the unrotated page: x' = 1754 - y, y' = x in djvu (bottom-up) coordinates. */
    @Test
    fun djvuRotatedPageMapsAreasLikeTheRenderedPage() {
        val document = open(BookFile(LINKS_ROTATED_DJVU))
        val pageSize = document.withPage(0) { getPageSize() }
        assertEquals(1754, pageSize.width)
        assertEquals(1240, pageSize.height)

        val links = document.links(0)
        assertEquals("$links", 3, links.size)
        val external = links[0]
        assertTrue(external.target is LinkTarget.External)
        assertRect(500f, 340f, 560f, 1090f, external)
        assertRect(700f, 340f, 760f, 1090f, links[1])
        assertRect(1104f, 940f, 1154f, 1090f, links[2])
        links.forEach {
            assertTrue("Inside the rotated page: $it", it.right <= pageSize.width && it.bottom <= pageSize.height)
        }
        assertTrue(links.findAt(530f, 700f) === external)
        assertNull(links.findAt(530f, 200f))
    }

    private fun assertRect(left: Float, top: Float, right: Float, bottom: Float, link: PageLink) {
        assertEquals("left of $link", left, link.left, 0.01f)
        assertEquals("top of $link", top, link.top, 0.01f)
        assertEquals("right of $link", right, link.right, 0.01f)
        assertEquals("bottom of $link", bottom, link.bottom, 0.01f)
    }

    companion object {
        const val LINKS_DJVU = "custom/links.djvu"
        const val LINKS_ROTATED_DJVU = "custom/links_rotated.djvu"
    }
}
