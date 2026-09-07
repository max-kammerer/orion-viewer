package universe.constellation.orion.viewer.test.navigation

import android.graphics.RectF
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.document.LinkTarget
import universe.constellation.orion.viewer.document.PageLink
import universe.constellation.orion.viewer.document.withPage
import universe.constellation.orion.viewer.test.espresso.BaseViewerActivityTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.onActivity

/* sicp.pdf: page 2 (0-based) is a contents page whose first link leads to page 8. */
class LinkNavigationTest : BaseViewerActivityTest(BookDescription.SICP) {

    private val contentsPage = 2

    @Test
    fun followingLinkIsJumpWithReturn() {
        val link = firstInternalLink()
        assertEquals(8, (link.target as LinkTarget.Internal).page)

        onActivity { it.controller!!.followLink(link) }
        Espresso.onIdle()
        assertEquals(8, currentPage0)

        assertTrue(onActivity { it.controller!!.navigateBack() })
        Espresso.onIdle()
        assertEquals(0, currentPage0)
    }

    @Test
    fun tapOnLinkAreaFollowsIt() {
        jump { it.goToPage(contentsPage) }
        assertEquals(contentsPage, currentPage0)
        val link = firstInternalLink()

        val center = onActivity { activity ->
            val controller = activity.controller!!
            val pageView = controller.pageLayoutManager.activePages.first { it.pageNum == contentsPage }
            val sceneRect = pageView.getSceneRect(RectF(link.left, link.top, link.right, link.bottom))
            floatArrayOf(sceneRect.centerX(), sceneRect.centerY())
        }

        val found = onActivity { listOfNotNull(it.controller!!.findLinkAt(center[0], center[1])) }
        assertEquals("The link should be found under its own center", 1, found.size)
        assertEquals(link.target, found.single().target)

        assertTrue(onActivity { it.controller!!.openLinkAt(center[0], center[1]) })
        Espresso.onIdle()
        assertEquals(8, currentPage0)

        assertTrue(onActivity { it.controller!!.navigateBack() })
        Espresso.onIdle()
        assertEquals(contentsPage, currentPage0)
    }

    @Test
    fun tapOutsideLinksIsNotALink() {
        assertFalse(onActivity { it.controller!!.openLinkAt(1f, 1f) })
        assertEquals(0, currentPage0)
    }

    private fun firstInternalLink(): PageLink = onActivity {
        it.controller!!.document.withPage(contentsPage) { getLinks() }.first { link -> link.target is LinkTarget.Internal }
    }

    private fun jump(body: (Controller) -> Unit) {
        onActivity { body(it.controller!!) }
        Espresso.onIdle()
    }
}
