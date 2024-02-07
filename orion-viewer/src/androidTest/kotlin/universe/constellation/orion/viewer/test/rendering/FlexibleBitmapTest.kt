package universe.constellation.orion.viewer.test.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import org.junit.After
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.min
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookTest
import universe.constellation.orion.viewer.test.framework.DEFAULT_COLOR_DELTA
import universe.constellation.orion.viewer.test.framework.compareBitmaps
import universe.constellation.orion.viewer.view.ColorStuff
import java.nio.IntBuffer

class FlexibleBitmapTest(bookDescription: BookDescription) : BookTest(bookDescription) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Render page in {0} book")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.testData()
        }

        private val BITMAP_CACHE = BitmapCache(20)
        private val PAINTS = ColorStuff()

        private val screenRect = Rect(0,0,600, 800)
        private val pageWidth = Rect(0,0,600, 800)
    }

    private val flexibleBitmapPart: FlexibleBitmap = FlexibleBitmap(pageWidth, screenRect.centerX(), screenRect.centerY())
    private val flexibleBitmapFull: FlexibleBitmap = FlexibleBitmap(pageWidth, screenRect.width(), screenRect.height())

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

    private fun doTest(page: Int, colorDelta: Int = DEFAULT_COLOR_DELTA) {
        val simpleLayoutStrategy =
            SimpleLayoutStrategy.create(document)
        simpleLayoutStrategy.setViewSceneDimension(screenRect.width(), screenRect.height())
        val pos = LayoutPosition()
        simpleLayoutStrategy.reset(pos, page)
        val rendering = Rect(0, 0, pos.x.pageDimension, pos.y.pageDimension)

        val (part, partData) = render(flexibleBitmapPart, rendering, pos)
        val (full, fullData) = render(flexibleBitmapFull, rendering, pos)

        compareBitmaps(partData, fullData, screenRect.width(), colorDelta = colorDelta) {
            dumpBitmap("Part", part)
            dumpBitmap("Full", full)
            flexibleBitmapPart.bitmaps().forEachIndexed { i, b ->
                dumpBitmap("Part$i", b)
            }
        }
    }

    private fun render(adaptiveBitmap: FlexibleBitmap, rendering: Rect, pos: LayoutPosition): Pair<Bitmap, IntArray> {
        adaptiveBitmap.resize(pos.x.pageDimension, pos.y.pageDimension, BITMAP_CACHE)
        adaptiveBitmap.render(rendering, pos, pos.pageNumber, document, BITMAP_CACHE)

        val bitmap = Bitmap.createBitmap(rendering.width(), rendering.height(), android.graphics.Bitmap.Config.ARGB_8888)
        val canvasPart = Canvas(bitmap)
        adaptiveBitmap.draw(canvasPart, rendering, RectF(rendering), PAINTS.backgroundPaint, PAINTS.borderPaint)
        val partBuf = IntBuffer.allocate(bitmap.width*bitmap.height)
        bitmap.copyPixelsToBuffer(partBuf)
        return bitmap to partBuf.array()
    }


    @After
    fun after() {
        BITMAP_CACHE.free()
    }
}