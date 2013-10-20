package universe.constellation.orion.viewer.test

import junit.framework.Assert.*

/**
 * User: mike
 * Date: 19.10.13
 * Time: 14:37
 */

class SimpleTest : BaseTest() {

    fun testScip() {
        val doc = openTestDocument(BaseTestTrait.SCIP)
        assertNotNull(doc)
    }
}