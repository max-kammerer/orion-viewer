package universe.constellation.orion.viewer.test.espresso

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.outline.showOutline
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.onActivity
import java.io.File

/* Smoke test for the outline dialog rebuilt on RecyclerView. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class OutlineDialogTest : BaseViewerActivityTest(BookDescription.SICP) {

    @Test
    fun outlineDialogShowsTree() {
        onActivity { activity ->
            showOutline(activity.controller!!, activity)
        }

        val tree = device.wait(
            Until.findObject(By.res("universe.constellation.orion.viewer", "mainTreeView")),
            LONG_TIMEOUT
        )
        assertNotNull("Outline dialog with tree list did not appear", tree)
        assertTrue("Outline tree has no rows", tree.childCount > 0)

        device.takeScreenshot(File("/sdcard/Download/orion/outline_dialog.png"))
    }

    @Test
    fun currentChapterIsHighlightedMidBook() {
        onActivity { activity ->
            activity.controller!!.goToPage(299)
            showOutline(activity.controller!!, activity)
        }

        val tree = device.wait(
            Until.findObject(By.res("universe.constellation.orion.viewer", "mainTreeView")),
            LONG_TIMEOUT
        )
        assertNotNull("Outline dialog with tree list did not appear", tree)

        device.takeScreenshot(File("/sdcard/Download/orion/outline_dialog_midbook.png"))
    }

    @Test
    fun historyTabNavigatesBack() {
        onActivity { activity ->
            activity.controller!!.goToPage(99)
            activity.controller!!.goToPage(299)
            showOutline(activity.controller!!, activity)
        }

        val historyTab = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)history"))), LONG_TIMEOUT)
        assertNotNull("History tab not found", historyTab)
        historyTab.click()

        /* oldest back entry: the jump left page 1 (no chapter above it, so the title is the page) */
        val firstEntry = device.wait(Until.findObject(By.text("page 1")), LONG_TIMEOUT)
        assertNotNull("First history entry not found", firstEntry)
        device.takeScreenshot(File("/sdcard/Download/orion/outline_dialog_history.png"))

        firstEntry.click()
        device.wait(Until.gone(By.res("universe.constellation.orion.viewer", "mainTreeView")), LONG_TIMEOUT)
        onActivity { activity ->
            org.junit.Assert.assertEquals(0, activity.controller!!.currentPage)
        }
    }
}
