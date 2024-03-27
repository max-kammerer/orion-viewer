package universe.constellation.orion.viewer.test

import org.junit.Test
import universe.constellation.orion.viewer.test.espresso.BaseViewerActivityTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.checkTrue

class AGrantFilePermissionsTest : BaseViewerActivityTest(BookDescription.SICP) {
    @Test
    fun stub() {
        checkTrue(
            "Can't read file: ${bookDescription.asFile().name}",
            bookDescription.asFile().canRead()
        )
    }
}