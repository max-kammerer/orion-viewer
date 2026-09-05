package universe.constellation.orion.viewer.test.navigation

import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.DocPlace
import universe.constellation.orion.viewer.NavKind
import universe.constellation.orion.viewer.layout.CropMargins
import universe.constellation.orion.viewer.layout.CropMode
import universe.constellation.orion.viewer.test.espresso.BaseViewerActivityTest
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.BookSet
import universe.constellation.orion.viewer.test.framework.BookSetRunner
import universe.constellation.orion.viewer.test.framework.Books
import universe.constellation.orion.viewer.test.framework.onActivity

@RunWith(BookSetRunner::class)
@Books(BookSet.MAIN)
class NavigationHistoryActivityTest(book: BookFile) : BaseViewerActivityTest(book) {

    @Test
    fun backAndForwardAcrossJumps() {
        requireLastPage(5)
        assertEquals(0, currentPage0)

        jump { it.goToPage(3) }
        assertEquals(3, currentPage0)
        jump { it.goToPage(5) }
        assertEquals(5, currentPage0)

        assertTrue(navigate { it.navigateBack() })
        assertEquals(3, currentPage0)
        assertTrue(navigate { it.navigateBack() })
        assertEquals(0, currentPage0)
        assertFalse("Nothing to go back to", navigate { it.navigateBack() })

        assertTrue(navigate { it.navigateForward() })
        assertEquals(3, currentPage0)
        assertTrue(navigate { it.navigateForward() })
        assertEquals(5, currentPage0)
        assertFalse("Nothing to go forward to", navigate { it.navigateForward() })
    }

    @Test
    fun placeInsidePageSurvivesZoomChange() {
        requireLastPage(4)
        /* When the whole page fits the viewport, the vertical offset is centered
         * and the fraction has nothing to survive: make the page overflow first. */
        jump { it.changeZoom(30000) }

        jump { it.goTo(DocPlace(2, 0f, 0.5f)) }
        val left = place()
        assertEquals(2, left.page)
        assertEquals(0.5f, left.yFraction, 0.05f)

        jump { it.goToPage(4) }
        assertEquals(4, currentPage0)

        val zoomBefore = onActivity { it.controller!!.currentPageZoom }
        jump { it.changeZoom(45000) }
        Espresso.onIdle()
        assertTrue("Zoom didn't change", onActivity { it.controller!!.currentPageZoom } > zoomBefore)

        assertTrue(navigate { it.navigateBack() })
        val returned = place()
        assertEquals(2, returned.page)
        assertEquals("Fraction of the page should be kept after zoom", left.yFraction, returned.yFraction, 0.05f)
    }

    @Test
    fun seriesOfStepsIsOneJump() {
        requireLastPage(6)
        jump { it.goToPage(2, NavKind.STEP) }
        jump { it.goToPage(4, NavKind.STEP) }
        jump { it.goToPage(6, NavKind.STEP) }
        assertEquals(6, currentPage0)

        assertTrue(navigate { it.navigateBack() })
        assertEquals("Back should skip the steps and return to the series start", 0, currentPage0)
        assertFalse(navigate { it.navigateBack() })
    }

    @Test
    fun movingAwayStartsNewSeries() {
        requireLastPage(5)
        jump { it.goToPage(2, NavKind.STEP) }
        jump { it.drawNext() }
        assertEquals(3, currentPage0)
        jump { it.goToPage(5, NavKind.STEP) }

        assertTrue(navigate { it.navigateBack() })
        assertEquals("The page left by paging starts a new series", 3, currentPage0)
        assertTrue(navigate { it.navigateBack() })
        assertEquals(0, currentPage0)
    }

    @Test
    fun placeSurvivesCropChange() {
        requireLastPage(4)
        /* 400%: the page must overflow the viewport even after the crop cuts it down,
         * otherwise it gets centered and the fraction has nothing to survive. */
        jump { it.changeZoom(40000) }

        jump { it.goTo(DocPlace(2, 0f, 0.5f)) }
        val left = place()
        assertEquals(0.5f, left.yFraction, 0.05f)

        jump { it.goToPage(4) }
        jump { it.changeCropMargins(CropMargins(top = 20, bottom = 5, cropMode = CropMode.MANUAL.cropMode)) }
        Espresso.onIdle()

        assertTrue(navigate { it.navigateBack() })
        val returned = place()
        assertEquals(2, returned.page)
        assertEquals("Fraction of the whole page should be kept after crop", left.yFraction, returned.yFraction, 0.05f)
    }

    private fun requireLastPage(minLastPage0: Int) {
        assumeTrue("The book is too short for this scenario", lastPageNumber0 >= minLastPage0)
    }

    private fun jump(body: (Controller) -> Unit) {
        onActivity { body(it.controller!!) }
        Espresso.onIdle()
    }

    private fun navigate(body: (Controller) -> Boolean): Boolean {
        val result = onActivity { body(it.controller!!) }
        Espresso.onIdle()
        return result
    }

    private fun place(): DocPlace {
        Espresso.onIdle()
        return onActivity { it.controller!!.currentPlace()!! }
    }
}
