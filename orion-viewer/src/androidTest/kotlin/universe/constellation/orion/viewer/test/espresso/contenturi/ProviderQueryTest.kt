package universe.constellation.orion.viewer.test.espresso.contenturi

import android.content.ContentResolver
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import universe.constellation.orion.viewer.android.getFileInfo
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.SimpleFileProvider
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGeneratedFile

/**
 * The file metadata is asked in one query: for a provider over a network share every query is a
 * trip to the server. A provider rejecting a column still gets its other columns asked one by one.
 */
class ProviderQueryTest : BaseTest() {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val analytics = (context.applicationContext as OrionApplication).analytics

    private val providerUri: Uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
        .authority(universe.constellation.orion.viewer.test.BuildConfig.APPLICATION_ID + ".fileprovider").build()

    private fun queries(): Int =
        context.contentResolver.call(providerUri, SimpleFileProvider.METHOD_QUERY_COUNT, null, null)!!
            .getInt(SimpleFileProvider.KEY_COUNT)

    private fun uriOf(name: String): Uri = createContentIntentWithGeneratedFile(name).data!!

    @Before
    fun resetQueries() {
        context.contentResolver.call(providerUri, SimpleFileProvider.METHOD_RESET_QUERY_COUNT, null, null)
    }

    @Test
    fun allColumnsInOneQuery() {
        val info = getFileInfo(context, uriOf("sicp.network.pdf"), analytics)!!
        assertEquals("sicp.pdf", info.name)
        assertEquals(BookDescription.SICP.asFile().length(), info.size)
        assertEquals(BookDescription.SICP.asPath(), info.path)
        assertEquals(1, queries())
    }

    @Test
    fun rejectedProjectionFallsBackToSingleColumns() {
        val info = getFileInfo(context, uriOf("sicp.nodata.pdf"), analytics)!!
        assertEquals("sicp.pdf", info.name)
        /* No _data, so the path and the size come from the descriptor: the file the provider generated. */
        assertTrue(info.path, info.path.endsWith("sicp.nodata.pdf"))
        assertTrue(info.size > 0)
        assertEquals("The rejected query and one per column", 4, queries())
    }
}
