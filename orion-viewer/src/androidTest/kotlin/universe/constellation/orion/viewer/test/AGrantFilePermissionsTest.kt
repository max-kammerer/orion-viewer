package universe.constellation.orion.viewer.test

import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.espresso.BaseViewerActivityTest
import universe.constellation.orion.viewer.test.framework.BookDescription

class AGrantFilePermissionsTest : BaseViewerActivityTest(BookDescription.SICP) {
    @Test
    fun stub() {
        assertTrue(BookDescription.SICP.asFile().canRead())
    }
}