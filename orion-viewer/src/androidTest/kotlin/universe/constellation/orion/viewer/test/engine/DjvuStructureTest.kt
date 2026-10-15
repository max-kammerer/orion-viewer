package universe.constellation.orion.viewer.test.engine

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import java.io.File

/**
 * The problem report on a DjVu document names its IFF form, the magic and size left out; the
 * error on a file that fails to open as DjVu shows what the file really starts with.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class DjvuStructureTest : BaseTest() {

    /* djvulibre's message says nothing of the format, the header does. Recognized formats named
       .djvu open by their content (OpenByContentTest); text still goes by the name, like the
       49-byte server message from a report. */
    @Test
    fun textNamedDjvuShowsItsHeader() {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "message.djvu")
        file.writeText("No download available for this file, try again later")
        val message = try {
            FileUtil.openFile(file).destroy()
            null
        } catch (e: Exception) {
            e.message
        } finally {
            file.delete()
        }
        assertTrue("header missing in: $message", message?.contains("header=No downl") == true)
        assertFalse("Not a DjVu, no form: $message", message!!.contains("form="))
    }

    @Test
    fun bundledBooksAreDjvm() {
        listOf(BookDescription.ALICE, BookDescription.DJVU_SPEC).forEach { book ->
            val document = book.openBook()
            try {
                assertEquals(book.toString(), "header=AT&TFORM, form=DJVM, content=djvu", document.describeStructure())
            } finally {
                document.destroy()
            }
        }
    }
}
