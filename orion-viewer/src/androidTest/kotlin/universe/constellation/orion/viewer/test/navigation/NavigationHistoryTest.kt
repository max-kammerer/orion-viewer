package universe.constellation.orion.viewer.test.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.NavigationHistory
import universe.constellation.orion.viewer.DocPlace as Place

class NavigationHistoryTest {

    @Test
    fun backAndForwardWalkTheJumps() {
        val history = NavigationHistory()
        history.leave(Place(0))
        history.leave(Place(5, 0f, 0.5f))

        assertEquals(Place(5, 0f, 0.5f), history.back(Place(9)))
        assertEquals(Place(0), history.back(Place(5, 0f, 0.5f)))
        assertNull(history.back(Place(0)))

        assertEquals(Place(5, 0f, 0.5f), history.forward(Place(0)))
        assertEquals(Place(9), history.forward(Place(5, 0f, 0.5f)))
        assertNull(history.forward(Place(9)))
    }

    @Test
    fun newJumpDropsForwardBranch() {
        val history = NavigationHistory()
        history.leave(Place(0))
        history.back(Place(5))
        assertTrue(history.canGoForward)

        history.leave(Place(0))
        assertFalse(history.canGoForward)
    }

    @Test
    fun leavingTheSamePlaceTwiceIsRememberedOnce() {
        val history = NavigationHistory()
        history.leave(Place(3, 0f, 0.25f))
        history.leave(Place(3, 0f, 0.251f))

        assertEquals(Place(3, 0f, 0.25f), history.back(Place(7)))
        assertFalse(history.canGoBack)
    }

    @Test
    fun oldestEntriesAreDroppedBeyondTheLimit() {
        val history = NavigationHistory(limit = 2)
        history.leave(Place(1))
        history.leave(Place(2))
        history.leave(Place(3))

        assertEquals(Place(3), history.back(Place(4)))
        assertEquals(Place(2), history.back(Place(3)))
        assertFalse(history.canGoBack)
    }

    @Test
    fun serializationRoundTrip() {
        val history = NavigationHistory()
        history.leave(Place(1, 0.1f, 0.2f))
        history.leave(Place(2))
        history.back(Place(3, 0.5f, 0.75f))

        val restored = NavigationHistory().apply { restore(history.serialize()) }
        assertEquals(history.serialize(), restored.serialize())
        assertEquals(Place(3, 0.5f, 0.75f), restored.forward(Place(2)))
        assertEquals(Place(2), restored.back(Place(3, 0.5f, 0.75f)))
        assertEquals(Place(1, 0.1f, 0.2f), restored.back(Place(2)))
    }

    @Test
    fun garbageIsIgnoredOnRestore() {
        val history = NavigationHistory().apply { restore("1:0:0,broken,x:y:z|") }
        assertFalse(history.canGoForward)
        assertEquals(Place(1), history.back(Place(2)))
        assertFalse(history.canGoBack)

        NavigationHistory().apply { restore("") }.let {
            assertFalse(it.canGoBack)
        }
    }
}
