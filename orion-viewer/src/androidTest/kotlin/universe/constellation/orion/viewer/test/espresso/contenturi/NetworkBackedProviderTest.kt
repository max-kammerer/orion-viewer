package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGeneratedFile
import universe.constellation.orion.viewer.test.framework.onActivity

/**
 * A provider over a network share touches the network in query(). Asked from the viewer's main
 * thread it dies with NetworkOnMainThreadException, since StrictMode's thread policy travels with
 * the binder call, and the intent failed with "Error during intent processing". Asked in the
 * background it answers, and the path it gives opens the book directly, without the copy dialog.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class NetworkBackedProviderTest : BaseTestWithActivity(createContentIntentWithGeneratedFile("sicp.network.pdf")) {

    @Test
    fun bookFromNetworkBackedProviderOpens() {
        Espresso.onIdle()
        onActivity {
            val controller = it.controller
            assertNotNull("The book should open directly", controller)
            assertEquals(BookDescription.SICP.asPath(), controller!!.document.filePath)
        }
    }
}
