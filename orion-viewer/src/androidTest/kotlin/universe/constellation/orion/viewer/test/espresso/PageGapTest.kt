package universe.constellation.orion.viewer.test.espresso

import android.graphics.PointF
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.util.MoveUtil
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.view.PAGE_GAP
import universe.constellation.orion.viewer.view.pageEndY
import kotlin.math.floor
import kotlin.math.round

/**
 * Pages in the continuous mode stand at whole pixels with exactly [PAGE_GAP] between them, after
 * any scroll and zoom: a page at a fractional position is blurred by the bitmap filter and its
 * border falls differently on every boundary; a zoom rounding page sizes apart from their
 * positions leaves gaps of other widths, or a 1 px hole showing the background when the gap is 0.
 */
class PageGapTest : BaseViewerActivityTest(BookFile(BaseTest.SICP)) {

    /** A pinch without a finger move, finished at [focus]: returns the vertical offset the scene drew it with. */
    private fun pinch(controller: Controller, zoom: Float, focus: PointF): Float {
        val offsetX = MoveUtil.calcOffset(focus.x, focus.x, zoom, false)
        val offsetY = MoveUtil.calcOffset(focus.y, focus.y, zoom, false)
        controller.translateAndZoom(zoom, focus, offsetX, offsetY)
        return offsetY
    }

    private fun assertLayout(step: String) {
        Espresso.onIdle()
        onActivity {
            val pages = it.controller!!.pageLayoutManager.activePages.sortedBy { page -> page.pageNum }
            assertTrue("$step: ${pages.size} pages", pages.size >= 2)
            pages.forEach { page ->
                val y = page.layoutData.position.y
                assertEquals("$step: page ${page.pageNum} at $y", floor(y), y, 0f)
            }
            pages.zipWithNext { a, b ->
                if (b.pageNum == a.pageNum + 1) {
                    assertEquals(
                        "$step: gap between ${a.pageNum} and ${b.pageNum}",
                        PAGE_GAP.toFloat(), b.layoutData.position.y - a.pageEndY, 0f
                    )
                }
            }
        }
    }

    @Test
    fun pagesStayAtWholePixelsWithTheSameGap() {
        Espresso.onIdle()
        onActivity {
            val manager = it.controller!!.pageLayoutManager
            repeat(9) { manager.doScroll(0f, 0f, 0f, -137.37f) }
        }
        assertLayout("scroll")

        listOf(1.37f, 0.61f, 2.13f).forEach { zoom ->
            onActivity {
                pinch(it.controller!!, zoom, PointF(301.3f, 517.7f))
            }
            assertLayout("zoom $zoom")

            onActivity {
                it.controller!!.pageLayoutManager.doScroll(0f, 0f, 0f, 41.29f)
            }
            assertLayout("scroll after zoom $zoom")
        }
    }

    /* A pinch in the gap has no page under it: the nearest one keeps its place, not the first
       active page, which may be off the screen. */
    @Test
    fun pinchInTheGapKeepsTheNearestPage() {
        Espresso.onIdle()
        /* big enough for the gap to change by more than the rounding: 2 px become 6 */
        val zoom = 3f
        val (pageNum, expected) = onActivity {
            val manager = it.controller!!.pageLayoutManager
            repeat(9) { manager.doScroll(0f, 0f, 0f, -137f) }
            val page = manager.activePages.first { page -> page.layoutData.position.y > 0 }
            val y = page.layoutData.position.y
            val focus = PointF(301f, y - 0.5f)
            val offsetY = pinch(it.controller!!, zoom, focus)
            page.pageNum to round(y * zoom - offsetY)
        }
        assertLayout("zoom in the gap")
        onActivity {
            val page = it.controller!!.pageLayoutManager.activePages.first { page -> page.pageNum == pageNum }
            assertEquals("page $pageNum under the pinch", expected, page.layoutData.position.y, 0f)
        }
    }
}
