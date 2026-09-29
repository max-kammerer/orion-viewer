package universe.constellation.orion.viewer.test

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.android.describeEmptyFile
import universe.constellation.orion.viewer.test.framework.BaseTest
import java.io.File

/**
 * The report on an empty book tells a truly empty file from one the provider knows to be bigger
 * (a stale path, a download still being written), without naming it.
 */
class EmptyFileReportTest : BaseTest() {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val uri = Uri.parse("content://media/external/file/49753")

    @Test
    fun emptyOnDiskButNotForTheProvider() {
        val file = File(context.cacheDir, "Secret title.djvu").apply { writeBytes(ByteArray(0)) }
        try {
            val now = System.currentTimeMillis()
            val info = FileInfo("Secret title.djvu", 3264236, "49753", file.path, uri, lastModified = now - 5000, pathOrigin = "data")
            val description = info.describeEmptyFile(file, now)
            listOf("host=media", "path=data", "ext=djvu", "providerSize=3264236", "providerModified=5s ago", "exists=true", "canRead=true").forEach {
                assertTrue("'$it' missing in: $description", description.contains(it))
            }
            assertFalse("The file name leaked into: $description", description.contains("Secret"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun missingFile() {
        val file = File(context.cacheDir, "gone.pdf")
        val description = FileInfo(null, 0, "49753", file.path, uri).describeEmptyFile(file)
        listOf("path=none", "providerSize=0", "providerModified=unknown", "fileModified=unknown", "exists=false").forEach {
            assertTrue("'$it' missing in: $description", description.contains(it))
        }
    }
}
