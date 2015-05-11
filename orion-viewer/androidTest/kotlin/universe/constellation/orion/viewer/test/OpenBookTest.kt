package universe.constellation.orion.viewer.test

import junit.framework.Assert.*

/**
 * User: mike
 * Date: 19.10.13
 * Time: 14:37
 */

class OpenBookTest : BaseTest() {

    fun testOpenScip() {
        val doc = openTestBook(TestUtil.SICP)
        assertNotNull(doc)
        assertEquals(762, doc.getPageCount())
        doc.destroy()
    }

    fun testOpenAlice() {
        val doc = openTestBook(TestUtil.ALICE)
        assertNotNull(doc)
        assertEquals(77, doc.getPageCount())
        doc.destroy()
    }
}