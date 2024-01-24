package universe.constellation.orion.viewer.test.utils

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RectInvariantTest {

    @Test
    fun onePointRect() {
        val rect = Rect(1, 1, 2, 2)
        assertEquals(1, rect.width())
        assertEquals(1, rect.height())
        assertTrue(rect.contains(1, 1))
        assertFalse(rect.contains(2, 2))

        val rect2 = Rect(rect)
        assertTrue(rect.intersect(rect2))
        assertEquals(1, rect.width())
        assertEquals(1, rect.height())
    }

    @Test
    fun emptyRect() {
        val rect = Rect(1, 1, 1, 1)
        assertEquals(0, rect.width())
        assertEquals(0, rect.height())
        assertFalse(rect.contains(1, 1))
        assertFalse(rect.contains(2, 2))

        val rect2 = Rect(rect)
        assertFalse(rect.intersect(rect2))
    }

    @Test
    fun emptyIntersection() {
        val rect = Rect(1, 1, 2, 2)
        val rect2 = Rect(2, 2, 3, 3)
        assertFalse(rect.intersect(rect2))
    }
}