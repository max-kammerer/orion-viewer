package universe.constellation.orion.viewer.test.engine

import android.graphics.Bitmap
import android.graphics.Color
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.document.NonFatalErrorReporter
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookFile

/**
 * A standalone image (or a comic page) whose data mupdf can't decode opens fine, but loading its
 * page throws. The page must degrade to a blank one with [loadError] set rather than throw from
 * every caller: in 0.97.0 it crashed text selection on double tap on the UI thread. The files
 * reproduce the two messages from those crash reports. They live in the broken/ subfolder, so
 * the folder-scanning suites don't pick them up.
 */
@RunWith(Parameterized::class)
class BrokenImagePageTest(private val file: String, private val expectedError: String) : BaseTest() {

    companion object {
        /** The first four bytes of a jpeg wiped, so the format isn't recognized at all. */
        const val BROKEN_JPEG = "broken/broken_header.jpg"

        /** A valid tiff header and IFD without strip, tile or jpeg data. */
        const val BROKEN_TIFF = "broken/missing_strips.tiff"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<Array<Any>> = listOf(
            /* Not recognized at all: the message shows the first bytes. */
            arrayOf(BROKEN_JPEG, "unknown image file format (header 00 00 00 00 00 10 4a 46 49 46 00 01)"),
            arrayOf(BROKEN_TIFF, "image is missing strip, tile and jpeg data"),
            /* Formats mupdf doesn't decode, under a name it does: named in the message. */
            arrayOf("broken/heic_photo.jpg", "unknown image file format (HEIC)"),
            arrayOf("broken/avif_picture.jpg", "unknown image file format (AVIF)"),
            arrayOf("broken/jpegxl_page.png", "unknown image file format (JPEG XL)"),
        )
    }

    private val reports = CopyOnWriteArrayList<Pair<String, Throwable>>()

    private val reporter = NonFatalErrorReporter { message, error -> reports.add(message to error) }

    @Test
    fun brokenPageDegradesToBlankPage() {
        val document = FileUtil.openFile(BookFile(file).asFile(), reporter)
        try {
            assertEquals(1, document.pageCount)
            val page = document.getOrCreatePageAdapter(0)
            try {
                page.readPageDataForRendering()
                assertEquals(expectedError, page.loadError)

                val size = page.getPageSize()
                assertTrue("Stub size expected, got $size", size.width > 0 && size.height > 0)
                assertNull(page.getPageText())
                assertNull(page.searchText("broken"))
                assertTrue(page.getLinks().isEmpty())

                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.renderPage(bitmap, 1.0, 0, 0, size.width, size.height, 0, 0)
                assertEquals("Nothing to render on a broken page", Color.WHITE, bitmap.getPixel(size.width / 2, size.height / 2))

                /* Not retried: the second call must neither throw nor reset the error. */
                page.readPageDataForRendering()
                assertEquals(expectedError, page.loadError)
            } finally {
                page.destroy()
            }

            assertEquals("One report per broken page: $reports", 1, reports.size)
            val (message, error) = reports.single()
            val extension = file.substringAfterLast('.')
            assertEquals("Page 1 of a .$extension book can't be loaded", message)
            assertEquals(expectedError, error.message)

            /* A page adapter recreated, as on scrolling back, fails again but isn't reported twice. */
            val again = document.getOrCreatePageAdapter(0)
            try {
                again.readPageDataForRendering()
                assertEquals(expectedError, again.loadError)
            } finally {
                again.destroy()
            }
            assertEquals("Reported once per document: $reports", 1, reports.size)
        } finally {
            document.destroy()
        }
    }
}
