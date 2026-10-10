package universe.constellation.orion.viewer.test.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BitmapCache
import universe.constellation.orion.viewer.bitmap.FlexibleBitmap
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.compareBitmaps
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.resetNoAutoCrop
import java.nio.IntBuffer

/**
 * Page images in the codecs of scanned books (JPEG 2000, CCITT G4, JBIG2): the page renders, and
 * the tiled render equals the full one, which exercises decoding a part of an image per tile and
 * the whole-image decode shared by the tiles. See
 * testData/codecs/README.md for the files.
 */
@RunWith(Parameterized::class)
class CodecRenderTest(private val file: String) : BaseTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<String> = listOf(
            "codecs/jpx_gray.pdf",
            "codecs/jpx_rgb.pdf",
            "codecs/jpx_alpha.pdf",
            "codecs/ccitt_g4.pdf",
            "codecs/jbig2_annex_h.pdf",
            /* a real scan from archive.org: JPEG 2000 background and foreground, JBIG2 mask, OCR text */
            "codecs/ia_mrc_jbig2_jpx.pdf",
            /* bug reproductions from PDFium, of other sizes */
            "codecs/pdfium_bug_631912.pdf",
            "codecs/pdfium_bug_304.pdf",
        )
    }

    private val bitmapCache = BitmapCache(20)
    private val paints = ColorStuff()
    private val screenRect = Rect(0, 0, 600, 800)

    @Test
    fun tilesMatchFullPage() {
        val document = BookFile(file).openBook()
        try {
            assertTrue(document.pageCount >= 1)
            val page = document.getOrCreatePageAdapter(0)
            val layout = SimpleLayoutStrategy.create()
            layout.setViewSceneDimension(screenRect.width(), screenRect.height())
            val pos = LayoutPosition()
            layout.resetNoAutoCrop(pos, page.pageNum, page.getPageSize(), true)
            val rendering = Rect(0, 0, pos.x.pageDimension, pos.y.pageDimension)
            val pageRect = Rect(0, 0, rendering.width(), rendering.height())

            val (part, partData) = render(FlexibleBitmap(pageRect, screenRect.centerX(), screenRect.centerY()), rendering, pos, page)
            val (full, fullData) = render(FlexibleBitmap(pageRect, screenRect.width(), screenRect.height()), rendering, pos, page)

            /* not blank: some pixels clearly off white (thin light strokes count too) */
            val marked = fullData.count { minOf(Color.red(it), Color.green(it), Color.blue(it)) < 200 }
            assertTrue("$file rendered blank", marked > 50)

            compareBitmaps(partData, fullData, rendering.width()) {
                dumpBitmap("Part", part)
                dumpBitmap("Full", full)
            }
            if (file.endsWith("jpx_alpha.pdf")) {
                /* "Text under the image" lies in the fully transparent band (rows 300..500): the
                   alpha channel must let it through. */
                val band = (380 until 440).sumOf { y -> (40 until 560).count { x ->
                    val c = fullData[y * rendering.width() + x]
                    Color.red(c) + Color.green(c) + Color.blue(c) < 3 * 96
                } }
                assertTrue("text under the transparent band is hidden ($band dark pixels)", band > 200)
            }
            page.destroy()
        } finally {
            document.destroy()
        }
    }

    private fun render(bitmap: FlexibleBitmap, rendering: Rect, pos: LayoutPosition, page: Page): Pair<Bitmap, IntArray> {
        bitmap.resize(pos.x.pageDimension, pos.y.pageDimension, bitmapCache)
        bitmap.enableAll(bitmapCache)
        runBlocking { bitmap.render(rendering, pos, page) }
        val result = Bitmap.createBitmap(rendering.width(), rendering.height(), Bitmap.Config.ARGB_8888)
        bitmap.draw(Canvas(result), rendering, paints.mainPagePaint)
        val buffer = IntBuffer.allocate(result.width * result.height)
        result.copyPixelsToBuffer(buffer)
        return result to buffer.array()
    }

    @After
    fun after() {
        bitmapCache.free()
    }
}
