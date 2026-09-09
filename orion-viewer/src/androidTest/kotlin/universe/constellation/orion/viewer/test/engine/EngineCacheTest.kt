package universe.constellation.orion.viewer.test.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.withPage
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription

class EngineCacheTest : BaseTest() {

    private fun Document.decode(pages: IntRange): List<String> = pages.map { p ->
        withPage(p) {
            readPageDataForRendering()
            getPageSize().let { "${it.width}x${it.height}" }
        }
    }

    private fun trimSurvives(document: Document) {
        try {
            assertTrue("$document should have a cache", document.cacheLimit > 0)
            val sizes = document.decode(0..4)
            document.trimCache(50)
            document.trimCache(0)
            assertEquals(sizes, document.decode(0..4))
        } finally {
            document.destroy()
        }
    }

    @Test
    fun pdfPagesSurviveCacheTrim() = trimSurvives(BookDescription.SICP.openBook())

    @Test
    fun djvuPagesSurviveCacheTrim() = trimSurvives(BookDescription.DJVU_SPEC.openBook())

    /* Decoded page files stay in the cache once their page adapter is gone, so the second pass
     * over the same pages is a lookup, not a decode: tens of times faster, 3x is the safe margin. */
    @Test
    fun djvuCacheKeepsDecodedPages() {
        val document = DjvuDocument(BookDescription.DJVU_SPEC.asPath(), 32L shl 20)
        try {
            val first = timed { document.decode(0..9) }
            val second = timed { document.decode(0..9) }
            assertTrue("cached pass ${second}ms vs first ${first}ms", second * 3 < first)
            document.trimCache(0)
            val afterTrim = timed { document.decode(0..9) }
            assertTrue("pass after trim ${afterTrim}ms vs cached ${second}ms", afterTrim > second * 3)
        } finally {
            document.destroy()
        }
    }

    private fun timed(body: () -> Unit): Long {
        val start = System.nanoTime()
        body()
        return (System.nanoTime() - start) / 1_000_000
    }
}
