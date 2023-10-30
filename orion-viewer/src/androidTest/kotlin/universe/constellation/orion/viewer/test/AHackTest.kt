package universe.constellation.orion.viewer.test

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.openTestBook

@RunWith(AndroidJUnit4::class)
class AHackTest : BaseTest() {

    @Test
    fun pageCountCheck() {
        try {
            openTestBook(BookDescription.SICP.path).pageCount
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Assert.assertTrue(
                    e.message,
                    e is RuntimeException && e.message?.contains("Permission denied") == true
                )
            } else {
                e.printStackTrace()
                Assert.fail(e.message)
            }
        }
    }

}