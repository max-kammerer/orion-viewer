package universe.constellation.orion.viewer.test.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.DocumentWithCaching
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.test.MANUAL_DEBUG
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookTest
import universe.constellation.orion.viewer.view.ColorStuff
import java.nio.IntBuffer

var screenRect = Rect(0,0,600, 800)
var pageWidth = Rect(0,0,600, 800)

class FlexibleBitmapTest(private val bookDescription: BookDescription) : BookTest(bookDescription) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Render page in {0} book")
        fun testData(): Iterable<Array<BookDescription>> {
            return BookDescription.testData()
        }

        val BITMAP_CACHE = BitmapCache(20)
        val PAINTS = ColorStuff()
    }

    private val flexibleBitmapPart: FlexibleBitmap = FlexibleBitmap(pageWidth, screenRect.centerX(), screenRect.centerY())
    private val flexibleBitmapFull: FlexibleBitmap = FlexibleBitmap(pageWidth, screenRect.width(), screenRect.height())

    @Test
    fun test1Page() {
        doTest(0)
    }

    @Test
    fun test3Page() {
        doTest(2)
    }

    private fun doTest(page: Int) {
        val simpleLayoutStrategy =
            SimpleLayoutStrategy.create(document as DocumentWithCaching)
        simpleLayoutStrategy.setViewSceneDimension(screenRect.width(), screenRect.height())
        val pos = LayoutPosition()
        simpleLayoutStrategy.reset(pos, page)
        val rendering = Rect(0, 0, pos.x.pageDimension, pos.y.pageDimension)

        val (part, partData) = render(flexibleBitmapPart, rendering, pos)
        val (full, fullData) = render(flexibleBitmapFull, rendering, pos)


        if (MANUAL_DEBUG && !partData.contentEquals(fullData)) {
            dumpBitmap("Part", part)
            dumpBitmap("Full", full)
            flexibleBitmapPart.bitmaps().forEachIndexed { i, b ->
                dumpBitmap("Part$i", b)
            }
        }

        Assert.assertArrayEquals(partData, fullData)
    }

    private fun render(adaptiveBitmap: FlexibleBitmap, rendering: Rect, pos: LayoutPosition): Pair<Bitmap, IntArray> {
        adaptiveBitmap.resize(pos.x.pageDimension, pos.y.pageDimension, BITMAP_CACHE)
        adaptiveBitmap.render(rendering, pos, 0, document, BITMAP_CACHE)

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