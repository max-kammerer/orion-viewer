package universe.constellation.orion.viewer.test.espresso

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.lastPageNum0
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.onActivity


@RunWith(Parameterized::class)
class RenderFastTest(bookDescription: BookFile): BaseViewerActivityTest(bookDescription) {

    @Test
    fun renderForward() {
        val lastPage = onActivity {
            it.controller!!.lastPageNum0
        }
        for (i in 0 ..lastPage) {
            testGotoSwipe(i)
        }
    }

    @Test
    fun renderBackward() {
        val lastPage = onActivity {
            it.controller!!.lastPageNum0
        }
        for (i in lastPage  downTo 0) {
            testGotoSwipe(i)
        }
    }


    private fun testGotoSwipe(page: Int) {
        onActivity {
            it.controller!!.drawPage(page, 0, 0, true)!!
            //assertEquals(page, it.controller!!.currentPage)
        }
    }
}
