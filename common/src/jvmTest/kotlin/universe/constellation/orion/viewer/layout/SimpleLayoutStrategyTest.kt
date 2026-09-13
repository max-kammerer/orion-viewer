package universe.constellation.orion.viewer.layout

import universe.constellation.orion.viewer.PageInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleLayoutStrategyTest {

    private fun strategy(width: Int, height: Int): LayoutStrategy =
        SimpleLayoutStrategy.create().apply { setViewSceneDimension(width, height) }

    @Test
    fun fitWidthScalesThePageToTheScreen() {
        val layout = strategy(320, 600)
        layout.changeZoom(0)
        val pos = LayoutPosition()
        layout.reset(pos, PageInfo(0, 640, 900))
        assertEquals(320, pos.x.pageDimension)
        assertEquals(450, pos.y.pageDimension)
        assertEquals(0.5, pos.docZoom, 0.0001)
    }

    @Test
    fun fitPageNeverExceedsTheScreen() {
        val layout = strategy(320, 600)
        layout.changeZoom(-2)
        val pos = LayoutPosition()
        layout.reset(pos, PageInfo(0, 1000, 500))
        assertTrue(pos.x.pageDimension <= 320)
        assertTrue(pos.y.pageDimension <= 600)
    }

    @Test
    fun screenPointIsPageOffsetPlusMargins() {
        val layout = strategy(320, 600)
        layout.changeZoom(0)
        val pos = LayoutPosition()
        layout.reset(pos, PageInfo(0, 640, 900))
        val point = layout.convertToPoint(pos)
        assertEquals(pos.x.marginLeft + pos.x.offset, point.x)
        assertEquals(pos.y.marginLeft + pos.y.offset, point.y)
    }
}
