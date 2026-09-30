package universe.constellation.orion.viewer.test.engine

import android.os.Build
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookFile

/**
 * The problem report on a DjVu document names its IFF form, the magic and size left out; the
 * error on a file that fails to open as DjVu shows what the file really starts with.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class DjvuStructureTest : BaseTest() {

    /* djvulibre's message says nothing of the format, the header does. */
    @Test
    fun otherFormatsNamedDjvuShowTheirHeader() {
        mapOf(
            "broken/pdf_named.djvu" to listOf("IFFByteStream not ready", "header=%PDF-1.4"),
            "broken/png_named.djvu" to listOf("Corrupted IFF file", "header=.PNG...."),
        ).forEach { (file, expected) ->
            val message = try {
                BookFile(file).openBook().destroy()
                null
            } catch (e: Exception) {
                e.message
            }
            expected.forEach {
                assertTrue("'$it' missing in: $message", message?.contains(it) == true)
            }
            assertFalse("Not a DjVu, no form: $message", message!!.contains("form="))
        }
    }

    @Test
    fun bundledBooksAreDjvm() {
        listOf(BookDescription.ALICE, BookDescription.DJVU_SPEC).forEach { book ->
            val document = book.openBook()
            try {
                assertEquals(book.toString(), "header=AT&TFORM, form=DJVM", document.describeStructure())
            } finally {
                document.destroy()
            }
        }
    }
}
