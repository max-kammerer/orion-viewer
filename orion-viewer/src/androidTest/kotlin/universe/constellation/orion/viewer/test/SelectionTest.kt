package universe.constellation.orion.viewer.test

import junit.framework.Assert
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.TestUtil

class SelectionTest : BaseTest() {

    fun testSicp() {
        val book = openTestBook(TestUtil.SICP)
        val text = book.getText(11/*zero based*/, 242, 172, 140, 8, false)

        Assert.assertEquals("These programs", text)
    }

    fun testSicpSingleWord() {
        val book = openTestBook(TestUtil.SICP)
        val selectionRect = SelectionAutomata.getSelectionRectangle(250, 176, 0, 0, true)
        val text = book.getText(11/*zero based*/, selectionRect.left, selectionRect.top, selectionRect.width(), selectionRect.height(), true)

        Assert.assertEquals("These", text)
    }

    fun testAlice() {
        val book = openTestBook(TestUtil.ALICE)
        val text = book.getText(5/*zero based*/, 1288, 517, 115, 50, false)

        Assert.assertEquals("tunnel ", text)
    }

    fun testAliceSingleWord() {
        val book = openTestBook(TestUtil.ALICE)
        val selectionRect = SelectionAutomata.getSelectionRectangle(518, 556, 0, 0, true)
        val text = book.getText(5/*zero based*/, selectionRect.left, selectionRect.top, selectionRect.width(), selectionRect.height(), true)

        Assert.assertEquals("The ", text)
    }

}