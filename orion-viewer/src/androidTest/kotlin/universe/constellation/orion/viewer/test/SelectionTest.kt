package universe.constellation.orion.viewer.test

import junit.framework.Assert
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.TestUtil

/**
 * Created by mike on 25.10.15.
 */

class SelectionTest() : BaseTest() {

    fun testSicp() {
        val book = openTestBook(TestUtil.SICP)
        val text = book.getText(11/*zero based*/, 242, 172, 140, 8)

        Assert.assertEquals(text, "These programs")
    }

    fun testAlice() {
        val book = openTestBook(TestUtil.ALICE)
        val text = book.getText(5/*zero based*/, 1288, 517, 115, 50)

        Assert.assertEquals(text, "tunnel ")
    }

}