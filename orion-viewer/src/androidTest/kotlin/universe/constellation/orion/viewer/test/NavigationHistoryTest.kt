package universe.constellation.orion.viewer.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import universe.constellation.orion.viewer.DocPlace
import universe.constellation.orion.viewer.NavigationHistory

class NavigationHistoryTest {

    private fun place(page: Int) = DocPlace(page)

    private fun historyOf(vararg pages: Int) = NavigationHistory().apply {
        pages.forEach { leave(place(it)) }
    }

    @Test
    fun entriesReflectStacks() {
        val history = historyOf(1, 2, 3)
        assertEquals(listOf(1, 2, 3), history.backEntries().map { it.page })
        assertEquals(emptyList<Int>(), history.forwardEntries().map { it.page })

        history.back(place(10))
        assertEquals(listOf(1, 2), history.backEntries().map { it.page })
        /* forward now leads to the place we left */
        assertEquals(listOf(10), history.forwardEntries().map { it.page })
    }

    @Test
    fun backToMovesIntermediateEntriesForward() {
        val history = historyOf(1, 2, 3)
        val target = history.backTo(0, place(10))
        assertEquals(1, target!!.page)
        assertEquals(emptyList<Int>(), history.backEntries().map { it.page })
        /* nearest first: 2, then 3, then the place we left */
        assertEquals(listOf(2, 3, 10), history.forwardEntries().map { it.page })
    }

    @Test
    fun forwardToMovesIntermediateEntriesBack() {
        val history = historyOf(1, 2, 3)
        history.backTo(0, place(10)) /* forward: 2, 3, 10 */

        val target = history.forwardTo(2, place(1))
        assertEquals(10, target!!.page)
        assertEquals(listOf(1, 2, 3), history.backEntries().map { it.page })
        assertEquals(emptyList<Int>(), history.forwardEntries().map { it.page })
    }

    @Test
    fun forwardToMiddleEntry() {
        val history = historyOf(1, 2, 3)
        history.backTo(0, place(10)) /* back: [], forward: 2, 3, 10 */

        val target = history.forwardTo(1, place(1))
        assertEquals(3, target!!.page)
        assertEquals(listOf(1, 2), history.backEntries().map { it.page })
        assertEquals(listOf(10), history.forwardEntries().map { it.page })
    }

    @Test
    fun backToInvalidIndex() {
        val history = historyOf(1)
        assertNull(history.backTo(5, place(10)))
        assertEquals(listOf(1), history.backEntries().map { it.page })
    }
}
