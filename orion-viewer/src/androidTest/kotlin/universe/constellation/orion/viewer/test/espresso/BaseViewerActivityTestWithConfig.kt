package universe.constellation.orion.viewer.test.espresso

import android.content.Intent
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.test.framework.BookFile

abstract class BaseViewerActivityTestWithConfig(
    bookDescription: BookFile,
    startIntent: Intent = bookDescription.toOpenIntent(),
    config: Configuration = DefaultConfig
) : BaseViewerActivityTest(bookDescription, startIntent, config) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book and config={1}")
        fun testDataWithConfigs(): List<Array<Any>> {
            return decartMult(BookFile.testEntriesWithCustoms(), allConfigs())
        }
    }
}