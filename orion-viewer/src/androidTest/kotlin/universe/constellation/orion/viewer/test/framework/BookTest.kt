package universe.constellation.orion.viewer.test.framework

import android.content.Intent
import android.graphics.Point
import android.net.Uri
import org.junit.After
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.FileUtil.openFile
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.ALICE
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.DJVU_SPEC
import universe.constellation.orion.viewer.test.framework.BaseTest.Companion.SICP
import java.io.File

@RunWith(Parameterized::class)
abstract class BookTest(protected val bookDescription: BookDescription) : BaseTest() {

    private val documentDelegate = lazy { bookDescription.openBook() }

    protected val document by documentDelegate

    @After
    fun close() {
        if (documentDelegate.isInitialized()) {
            document.destroy()
        }
    }
}

open class BookFile(private val simpleFileName: String) {
    fun toOpenIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                OrionViewerActivity::class.qualifiedName!!
            )
            data = Uri.fromFile(asFile())
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    fun asFile() = File(BaseTest.testFolder, simpleFileName)

    fun asPath() = asFile().absolutePath

    fun openBook() = openFile(asFile())

    override fun toString(): String {
        return "BookFile(path='$simpleFileName')"
    }

    companion object {

        private val EXTENDED_HARD_CODED_TEST = System.getenv("test.books.extended")?.toBoolean() ?: false

        fun testEntriesWithCustoms(): List<BookFile> {
            val files = BaseTest.testFolder.listFiles()
            if (files == null || files.isEmpty()) return hardCodedEntries()
            return files.mapNotNull {
                if (it.isDirectory) {
                    null
                } else {
                    BookFile(it.name)
                }
            }
        }

        private fun hardCodedEntries(): List<BookFile> {
            return (if (EXTENDED_HARD_CODED_TEST)
                listOf(
                ALICE,
                "an1.pdf",
                "cr1.pdf",
                "cr2.pdf",
                "decode.pdf",
                DJVU_SPEC,
                "h1.djvu",
                "oz1.pdf",
                "quest.pdf",
                SICP
            ) else {
                listOf(
                    ALICE,
                    DJVU_SPEC,
                    SICP
                )
            }).map {
                BookFile(it)
            }
        }
    }
}

sealed class BookDescription(
        path: String,
        val pageCount: Int,
        val title: String?,
        val topLevelOutlineItems: Int,
        val allOutlineItems: Int = topLevelOutlineItems,
        val pageSize: Point = Point(0, 0)
): BookFile(path) {

    data object SICP: BookDescription(BaseTest.SICP, 762, "", 15, 139, Point(662, 885))
    data object ALICE: BookDescription(BaseTest.ALICE, 77, null, 0,  pageSize = Point(2481, 3508))
    data object DJVU_SPEC: BookDescription(BaseTest.DJVU_SPEC, 71, null, 1, 100, Point(2539, 3295))

    companion object {
        private fun testEntries(): List<BookDescription> {
            return if (MANUAL_DEBUG) {
                listOf(SICP)
            } else {
                listOf(SICP, ALICE, DJVU_SPEC)
            }
        }

        fun testData(): Iterable<Array<BookDescription>> {
            return testEntries().map { arrayOf(it) }
        }
    }
}