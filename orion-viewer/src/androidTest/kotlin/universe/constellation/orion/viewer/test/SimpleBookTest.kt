package universe.constellation.orion.viewer.test

import android.graphics.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookTest

class SimpleBookTest(private val bookDescription: BookDescription) : BookTest(bookDescription) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Open book {0} with expected page count {1} and outlines {2}")
        fun testData(): Iterable<Array<Any>> {
            return BookDescription.values().map { arrayOf(it) }
        }
    }

    @Test
    fun pageCountCheck() {
        assertEquals(bookDescription.pageCount, document.pageCount)
    }

    @Test
    fun topOutlineCheck() {
        assertEquals(bookDescription.topLevelOutlineItems, document.outline?.filter { it.level == 0  }?.size ?: 0)
    }

    @Test
    fun allOutlineCheck() {
        assertEquals(bookDescription.allOutlineItems, document.outline?.size ?: 0)
    }

    @Test
    fun titleCheck() {
        assertEquals(bookDescription.title, document.title)
    }

    @Test
    fun pageSizeCheck() {
        (0..5).forEach {
            assertEquals("Check page size of $it page", bookDescription.pageSize, document.getPageInfo(0).run { Point(width, height) })
        }
    }

    @Test
    fun findNonExistingText() {
        val result = document.searchPage(0, "abcdefghjklm....")
        assertTrue("Search should return nothing", result.isNullOrEmpty())

    }
}