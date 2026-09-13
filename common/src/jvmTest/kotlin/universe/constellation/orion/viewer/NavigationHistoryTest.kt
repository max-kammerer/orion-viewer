package universe.constellation.orion.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationHistoryTest {

    @Test
    fun backAndForwardRestorePlaces() {
        val history = NavigationHistory()
        history.leave(DocPlace(1))
        history.leave(DocPlace(5))
        assertTrue(history.canGoBack)
        assertFalse(history.canGoForward)

        assertEquals(DocPlace(5), history.back(DocPlace(9)))
        assertEquals(DocPlace(1), history.back(DocPlace(5)))
        assertNull(history.back(DocPlace(1)))
        assertEquals(listOf(DocPlace(5), DocPlace(9)), history.forwardEntries())

        assertEquals(DocPlace(5), history.forward(DocPlace(1)))
        assertEquals(DocPlace(9), history.forward(DocPlace(5)))
        assertNull(history.forward(DocPlace(9)))
    }

    @Test
    fun newJumpDropsTheForwardBranch() {
        val history = NavigationHistory()
        history.leave(DocPlace(1))
        history.back(DocPlace(2))
        assertTrue(history.canGoForward)
        history.leave(DocPlace(3))
        assertFalse(history.canGoForward)
    }

    @Test
    fun samePlaceIsNotRecordedTwice() {
        val history = NavigationHistory()
        history.leave(DocPlace(4, 0.5f, 0.5f))
        history.leave(DocPlace(4, 0.505f, 0.5f))
        assertEquals(1, history.backEntries().size)
    }

    @Test
    fun historyIsBounded() {
        val history = NavigationHistory(limit = 3)
        (1..10).forEach { history.leave(DocPlace(it)) }
        assertEquals(listOf(DocPlace(8), DocPlace(9), DocPlace(10)), history.backEntries())
    }

    @Test
    fun placesSurviveSerialization() {
        val history = NavigationHistory()
        history.leave(DocPlace(2, 0.25f, 0.75f))
        history.leave(DocPlace(7))
        val restored = NavigationHistory().apply { restore(history.serialize()) }
        assertEquals(history.backEntries(), restored.backEntries())
        assertEquals(DocPlace(3, 0.1f, 0.2f), DocPlace.parse("3:0.1:0.2"))
        assertNull(DocPlace.parse("garbage"))
    }
}
