package universe.constellation.orion.viewer.test.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription

/** The problem report on a DjVu document names its IFF form, the magic and size left out. */
class DjvuStructureTest : BaseTest() {

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
