package universe.constellation.orion.viewer.test

import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase

class AGrantFilePermissionsTest() : InstrumentationTestCase(BookDescription.SICP.toOpenIntent()) {
    @Test
    fun stub() {
        assertTrue(BookDescription.SICP.asFile().canRead())
    }
}