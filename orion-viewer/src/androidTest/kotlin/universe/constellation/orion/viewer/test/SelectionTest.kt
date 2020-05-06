package universe.constellation.orion.viewer.test

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.test.framework.BookTest
import universe.constellation.orion.viewer.test.framework.TestUtil

class SelectionTest(path: String, val page1Based: Int, val absoluteRect: Rect, val isSingleWord: Boolean, val expectedText: String) : BookTest(path) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test text selection in {0} with singleWord={3} expected text {4}")
        fun testData(): Iterable<Array<Any>> {
            return listOf(
                    arrayOf(TestUtil.SICP, 12, Rect(242, 172, 242 + 140, 172 + 8), false, "These programs"),
                    arrayOf(TestUtil.SICP, 12, Rect(250, 176, 250 + 0, 176 + 0), true, "These"),
                    arrayOf(TestUtil.ALICE, 6, Rect(1288, 517, 1288 + 115, 517 + 50), false, "tunnel "),
                    arrayOf(TestUtil.ALICE, 6, Rect(518, 556, 518 + 0, 556 + 0), true, "The ")
            )
        }
    }


    @Test
    fun testSelection() {
        val selectionRect = SelectionAutomata.getSelectionRectangle(absoluteRect.left, absoluteRect.top, absoluteRect.width(), absoluteRect.height(), isSingleWord)
        val text = document.getText(page1Based - 1/*zero based*/, selectionRect.left, selectionRect.top, selectionRect.width(), selectionRect.height(), isSingleWord)
        assertEquals(expectedText, text)
    }
}