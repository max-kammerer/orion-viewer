package universe.constellation.orion.viewer.test.framework

import android.graphics.Point
import org.junit.After
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
abstract class BookTest(path: String) : BaseTest() {

    protected val document by lazy {  openTestBook(path) }

    @After
    fun close() {
        document.destroy()
    }
}

enum class BookDescription(
        val path: String,
        val pageCount: Int,
        val title: String?,
        val topLevelOutlineItems: Int,
        val allOutlineItems: Int = topLevelOutlineItems,
        val pageSize: Point = Point(0, 0)
) {
    SICP(TestUtil.SICP, 762, "", 15, 139, Point(662, 885)),
    ALICE(TestUtil.ALICE, 77, null, 0,  pageSize = Point(2481, 3508)),
    DJVU_SPEC(TestUtil.DJVU_SPEC, 71, null, 1, 100, Point(2539, 3295))
}