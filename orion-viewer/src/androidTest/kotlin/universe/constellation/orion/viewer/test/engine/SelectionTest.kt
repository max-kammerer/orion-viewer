package universe.constellation.orion.viewer.test.engine

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.test.framework.BookTest

class SelectionTest(
    path: String,
    private val page1Based: Int,
    private val absoluteRect: Rect,
    private val isSingleWord: Boolean,
    private val expectedText: String
) : BookTest(path) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test text selection in {0} with singleWord={3} expected text {4}")
        fun testData(): Iterable<Array<Any>> {
            return listOf(
                    arrayOf(SICP, 12, Rect(242, 172, 242 + 140, 172 + 8), false, "These programs"),
                    arrayOf(SICP, 12, Rect(250, 176, 250 + 0, 176 + 0), true, "These"),
                    arrayOf(ALICE, 6, Rect(1288, 517, 1288 + 115, 517 + 50), false, "tunnel "),
                    arrayOf(ALICE, 6, Rect(518, 556, 518 + 0, 556 + 0), true, "The ")
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