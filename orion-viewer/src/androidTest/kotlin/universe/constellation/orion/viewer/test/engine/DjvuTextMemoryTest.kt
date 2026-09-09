package universe.constellation.orion.viewer.test.engine

import android.os.Debug
import android.util.Log
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.document.withPage
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription

/* Native heap while text is pulled from every page of a djvu book over and over: the text
 * s-expressions libdjvu hands out stay rooted in the document until released, and before the
 * JNI released them a search pass over DjVu3Spec.djvu cost about 8 MB for good. The first round
 * may still grow the heap (miniexp block pool, djvu cache), the later ones must not. Details go
 * to logcat, tag DjvuTextMemory. */
class DjvuTextMemoryTest : BaseTest() {

    @Test
    fun repeatedSearchAndText() {
        val document = BookDescription.DJVU_SPEC.openBook()
        try {
            val pages = document.pageCount
            log("start")
            var afterFirstRound = 0.0
            repeat(ROUNDS) { round ->
                for (page in 0 until pages) {
                    document.withPage(page) { searchText("zzzz-not-there") }
                }
                val heap = log("search round ${round + 1}")
                if (round == 0) afterFirstRound = heap
            }
            val afterSearch = log("search done")
            repeat(ROUNDS) { round ->
                for (page in 0 until pages) {
                    document.withPage(page) { getPageText(); getLinks() }
                }
                log("text+links round ${round + 1}")
            }
            val afterText = log("text+links done")
            assertTrue("Search rounds leak: %.1f -> %.1f MB".format(afterFirstRound, afterSearch), afterSearch - afterFirstRound < LEAK_TOLERANCE_MB)
            assertTrue("Text rounds leak: %.1f -> %.1f MB".format(afterSearch, afterText), afterText - afterSearch < LEAK_TOLERANCE_MB)
        } finally {
            document.destroy()
        }
        log("after destroy")
    }

    private fun log(stage: String): Double {
        val mb = Debug.getNativeHeapAllocatedSize().toDouble() / BYTES_IN_MB
        Log.i(TAG, "%-22s native heap %6.1f MB".format(stage, mb))
        return mb
    }

    companion object {
        private const val TAG = "DjvuTextMemory"
        private const val ROUNDS = 5
        private const val BYTES_IN_MB = 1L shl 20
        /* Four leaking rounds were worth ~30 MB, so this catches the regression with room for noise. */
        private const val LEAK_TOLERANCE_MB = 4.0
    }
}
