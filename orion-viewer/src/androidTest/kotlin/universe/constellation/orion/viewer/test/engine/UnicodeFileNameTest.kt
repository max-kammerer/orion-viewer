package universe.constellation.orion.viewer.test.engine

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.FileUtil
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.BookTest
import java.io.File

class UnicodeFileNameTest(private val bookDesc: BookDescription, private val unicodeName: String) :
    BookTest(bookDesc) {

    companion object {

        private val names = listOf("\uD83D\uDCD6", "本", "책")

        @JvmStatic
        @Parameterized.Parameters(name = "UTF file name test for {0} file with new name {1}")
        fun testData(): Iterable<Array<Any>> {
            return BookDescription.testData().flatMap { book ->
                names.map { name ->
                    arrayOf(book, name)
                }
            }
        }

        internal var counter = 0
    }

    @Test
    fun testUtfFileName() {
        val originFile = bookDesc.asFile()
        var newFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "$unicodeName${++counter}.${originFile.extension}"
        )
        var utfDoc: Document? = null
        try {
            newFile = originFile.copyTo(newFile, overwrite = true)
            utfDoc = FileUtil.openFile(newFile)
            Assert.assertEquals(bookDesc.openBook().pageCount, utfDoc.pageCount)
        } finally {
            utfDoc?.destroy()
        }
    }

}