package universe.constellation.orion.viewer.test.espresso

import android.os.Build
import android.view.KeyEvent
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.onActivity

/** The search box keeps the previous query of the book and enter searches on. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class SearchQueryTest : BaseViewerActivityTest(BookDescription.SICP) {

    @Test
    fun queryIsKeptBetweenSearches() {
        val startPage = currentPage()
        openSearch().text = QUERY
        device.findObject(By.res(PACKAGE, "searchNext")).click()
        val hitPage = waitForPageChange(startPage)
        closeSearch()
        assertEquals(QUERY, onActivity { it.lastPageInfo!!.lastSearchQuery })
        assertEquals(QUERY, onActivity { it.globalOptions.LAST_SEARCH_QUERY.value })

        val field = openSearch()
        assertEquals(QUERY, field.text)
        //the restored query is selected: typing replaces it
        device.pressKeyCode(KeyEvent.KEYCODE_X)
        assertEquals("x", field.text)
        closeSearch()
        //nothing was searched for, the query stays
        assertEquals(QUERY, onActivity { it.lastPageInfo!!.lastSearchQuery })
        assertEquals(hitPage, currentPage())
    }

    @Test
    fun enterSearchesForTheNextHit() {
        val startPage = currentPage()
        openSearch().text = QUERY
        device.pressEnter()
        val firstHit = waitForPageChange(startPage)
        val place = onActivity { it.controller!!.currentPlace()!! }
        device.pressEnter()
        //the second enter goes on from the first hit
        assertTrue(waitFor { onActivity { it.controller!!.currentPlace() != place } })
        assertTrue("Went back from page $firstHit to ${currentPage()}", currentPage() >= firstHit)
        closeSearch()
    }

    private fun openSearch(): UiObject2 {
        onActivity { it.startSearch() }
        val field = device.wait(Until.findObject(By.res(PACKAGE, "searchText")), LONG_TIMEOUT)
        assertNotNull("Search dialog did not appear", field)
        return field
    }

    private fun closeSearch() {
        //the first back may only hide the keyboard
        repeat(2) {
            if (!device.hasObject(By.res(PACKAGE, "searchText"))) return
            device.pressBack()
            device.wait(Until.gone(By.res(PACKAGE, "searchText")), LONG_TIMEOUT / 2)
        }
        assertTrue("Search dialog is still open", !device.hasObject(By.res(PACKAGE, "searchText")))
    }

    private fun currentPage(): Int = onActivity { it.controller!!.currentPage }

    private fun waitForPageChange(from: Int): Int {
        assertTrue("No hit found for $QUERY after page $from", waitFor { currentPage() != from })
        return currentPage()
    }

    private fun waitFor(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + LONG_TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    companion object {
        private const val PACKAGE = "universe.constellation.orion.viewer"
        private const val QUERY = "lambda"
    }
}
