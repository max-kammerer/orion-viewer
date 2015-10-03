package universe.constellation.orion.viewer.test

import junit.framework.Assert.*

/**
 * User: mike
 * Date: 19.10.13
 * Time: 14:37
 */

class OpenBookTest : BaseTest() {

    fun testOpenScip() {
        openBook(TestUtil.SICP, 762)
    }

    fun testOpenAlice() {
        openBook(TestUtil.ALICE, 77)
    }

    private fun openBook(book: String, pageCount: Int) {
        val doc = openTestBook(book)
        assertNotNull(doc)
        assertEquals(pageCount, doc.pageCount)
        doc.destroy()
    }
}