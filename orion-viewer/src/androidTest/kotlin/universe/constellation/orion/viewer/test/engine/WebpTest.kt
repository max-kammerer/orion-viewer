package universe.constellation.orion.viewer.test.engine

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.AbstractPage
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile
import kotlin.math.abs

/**
 * WebP is decoded by libwebp, linked into mupdf by a local patch: pages of comic archives and
 * standalone images. WebP carries no resolution, so a page is 96 dpi: 600x800 px give 450x600 pt.
 * The files live in the webp/ subfolder, so the folder-scanning suites don't pick them up.
 */
class WebpTest : BaseTest() {

    @Test
    fun comicWithWebpPages() {
        val document = BookFile("webp/comic_webp.cbz").openBook()
        try {
            assertEquals("Two WebP pages and a PNG one", 3, document.pageCount)

            withPage(document.getOrCreatePageAdapter(0)) { page, bitmap ->
                assertEquals(PageSize(450, 600), page.getPageSize())
                assertColor("lossy, left half", Color.rgb(220, 30, 30), bitmap.getPixel(100, 300))
                assertColor("lossy, right half", Color.rgb(30, 30, 220), bitmap.getPixel(350, 300))
            }
            withPage(document.getOrCreatePageAdapter(1)) { _, bitmap ->
                assertColor("lossless, opaque square", Color.BLACK, bitmap.getPixel(225, 300))
                assertColor("lossless, transparent around", Color.WHITE, bitmap.getPixel(50, 50))
            }
            withPage(document.getOrCreatePageAdapter(2)) { _, bitmap ->
                assertColor("png page", Color.rgb(30, 160, 60), bitmap.getPixel(225, 300))
            }
        } finally {
            document.destroy()
        }
    }

    @Test
    fun standaloneWebpImage() {
        val document = BookFile("webp/image.webp").openBook()
        try {
            assertEquals(1, document.pageCount)
            withPage(document.getOrCreatePageAdapter(0)) { page, bitmap ->
                assertEquals(PageSize(225, 300), page.getPageSize())
                assertColor("image", Color.rgb(40, 160, 80), bitmap.getPixel(112, 150))
            }
        } finally {
            document.destroy()
        }
    }

    /* Animation needs libwebp's demux library, not linked: the page is shown as broken, clearly. */
    @Test
    fun animatedWebpIsReportedAsUnsupported() {
        val document = BookFile("webp/animated.webp").openBook()
        try {
            val page = document.getOrCreatePageAdapter(0)
            try {
                page.readPageDataForRendering()
                assertEquals("animated webp is not supported", page.loadError)
            } finally {
                page.destroy()
            }
        } finally {
            document.destroy()
        }
    }

    private fun withPage(page: AbstractPage, check: (AbstractPage, Bitmap) -> Unit) {
        try {
            val size = page.getPageSize()
            assertNull(page.loadError)
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.renderPage(bitmap, 1.0, 0, 0, size.width, size.height, 0, 0)
            check(page, bitmap)
        } finally {
            page.destroy()
        }
    }

    private fun assertColor(what: String, expected: Int, actual: Int, tolerance: Int = 40) {
        val close = abs(Color.red(expected) - Color.red(actual)) <= tolerance &&
                abs(Color.green(expected) - Color.green(actual)) <= tolerance &&
                abs(Color.blue(expected) - Color.blue(actual)) <= tolerance
        assertTrue("$what: expected #%06x, got #%06x".format(expected and 0xffffff, actual and 0xffffff), close)
    }
}
