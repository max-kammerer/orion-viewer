package universe.constellation.orion.viewer.test.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookTest
import universe.constellation.orion.viewer.test.framework.DEFAULT_COLOR_DELTA
import universe.constellation.orion.viewer.test.framework.compareBitmaps
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.resetNoAutoCrop
import java.nio.IntBuffer
import kotlin.math.min

class FlexibleBitmapTest(bookDescription: BookDescription) : BookTest(bookDescription) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Render page in {0} book")
        fun testData(): List<BookDescription> {
            return BookDescription.testData()
        }

        private val BITMAP_CACHE = BitmapCache(20)
        private val PAINTS = ColorStuff()

        private val screenRect = Rect(0, 0, 600, 800)
        private val pageRect = Rect(0, 0, 600, 800)
    }

    private val flexibleBitmapPart: FlexibleBitmap = FlexibleBitmap(pageRect, screenRect.centerX(), screenRect.centerY())
    private val flexibleBitmapFull: FlexibleBitmap = FlexibleBitmap(pageRect, screenRect.width(), screenRect.height())

    @Test
    fun test1Page() {
        doTest(0)
    }

    @Test
    fun test3Page() {
        doTest(min(3, bookDescription.pageCount))
    }

    @Test
    fun test60Page() {
        //TODO: investigate problem with color
        val colorDelta = if (BookDescription.SICP != bookDescription) DEFAULT_COLOR_DELTA else 15
        doTest(min(59, bookDescription.pageCount), colorDelta)
    }

    private fun doTest(pageNum: Int, colorDelta: Int = DEFAULT_COLOR_DELTA) {
        val page = document.getOrCreatePageAdapter(pageNum)
        val simpleLayoutStrategy = SimpleLayoutStrategy.create()
        simpleLayoutStrategy.setViewSceneDimension(screenRect.width(), screenRect.height())
        val pos = LayoutPosition()
        simpleLayoutStrategy.resetNoAutoCrop(pos, page.pageNum, page.getPageSize(), true)
        val rendering = Rect(0, 0, pos.x.pageDimension, pos.y.pageDimension)

        val (part, partData) = render(flexibleBitmapPart, rendering, pos, page)
        val (full, fullData) = render(flexibleBitmapFull, rendering, pos, page)

        compareBitmaps(partData, fullData, screenRect.width(), colorDelta = colorDelta) {
            dumpBitmap("Part", part)
            dumpBitmap("Full", full)
            flexibleBitmapPart.bitmaps().forEachIndexed { i, b ->
                dumpBitmap("Part$i", b)
            }
        }
        page.destroy()
    }

    private fun render(adaptiveBitmap: FlexibleBitmap, rendering: Rect, pos: LayoutPosition, page: Page): Pair<Bitmap, IntArray> {
        adaptiveBitmap.resize(pos.x.pageDimension, pos.y.pageDimension, BITMAP_CACHE)
        adaptiveBitmap.enableAll(BITMAP_CACHE)
        runBlocking {
            adaptiveBitmap.render(rendering, pos, page)
        }

        val bitmap = Bitmap.createBitmap(rendering.width(), rendering.height(), android.graphics.Bitmap.Config.ARGB_8888)
        val canvasPart = Canvas(bitmap)
        adaptiveBitmap.draw(canvasPart, rendering, PAINTS.backgroundPaint)
        val partBuf = IntBuffer.allocate(bitmap.width*bitmap.height)
        bitmap.copyPixelsToBuffer(partBuf)
        return bitmap to partBuf.array()
    }

    @Test
    fun checkPartCountTest() {
        val bm = FlexibleBitmap(pageRect, pageRect.width(), pageRect.height())
        checkPartCount(bm, 1)

        bm.resize(pageRect.width(), pageRect.height(), BITMAP_CACHE)
        checkPartCount(bm, 1)

        bm.resize(pageRect.width() - 1, pageRect.height() - 1, BITMAP_CACHE)
        checkPartCount(bm, 1)

        bm.resize(pageRect.width() + 1, pageRect.height() + 1, BITMAP_CACHE)
        checkPartCount(bm, 2, 2)
    }

    private fun checkPartCount(bm: FlexibleBitmap, expectedRows: Int, expectedCols: Int = expectedRows) {
        var counter = 0
        bm.forAllTest { ++counter }
        Assert.assertEquals(expectedRows * expectedCols, counter)
        Assert.assertEquals(expectedRows, bm.data.size)
        Assert.assertEquals(expectedCols, bm.data[0].size)
    }

    @After
    fun after() {
        BITMAP_CACHE.free()
    }
}