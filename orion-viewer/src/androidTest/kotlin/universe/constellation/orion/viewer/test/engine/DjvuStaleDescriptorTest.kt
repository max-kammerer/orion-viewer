package universe.constellation.orion.viewer.test.engine

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileUtil.openFile
import universe.constellation.orion.viewer.test.framework.BaseTest
import java.io.File
import java.io.FileDescriptor

/* A zero-length djvu drives libdjvu's ByteStream::create into the path where the
 * mmap attempt fails and closes the fd, after which the fallback fdopen()s the very
 * same, already freed number. Whatever another thread opened in the meantime gets
 * hijacked and later closed behind its back. In production that is the RenderThread's
 * fence fd from binder, and the outcome is an fdsan abort.
 *
 * Here a churn thread plays the victim: it keeps opening /dev/null while the empty
 * book is being opened, never closes anything itself, and afterwards checks that every
 * descriptor it holds is still alive. A descriptor that is gone (EBADF), or a number
 * that was handed out twice, proves a foreign close. The race is narrow, hence the
 * repetitions. */
class DjvuStaleDescriptorTest : BaseTest() {

    @Test
    fun emptyDjvuDoesNotCloseForeignDescriptors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emptyBook = File(context.cacheDir, "empty.djvu")
        emptyBook.delete()
        assertTrue("can't create ${emptyBook.absolutePath}", emptyBook.createNewFile())
        try {
            repeat(ITERATIONS) { iteration ->
                val victim = DescriptorChurn()
                victim.start()
                try {
                    try {
                        openFile(emptyBook).destroy()
                    } catch (e: Exception) {
                        /* Expected: an empty book can't be opened. */
                    }
                } finally {
                    val stolen = victim.stopAndRelease()
                    assertTrue(
                        "Iteration $iteration: descriptors closed behind the victim's back: $stolen",
                        stolen.isEmpty()
                    )
                }
            }
        } finally {
            emptyBook.delete()
        }
    }

    private class DescriptorChurn : Thread("fd-churn") {
        private val held = ArrayList<FileDescriptor>(CAP)

        @Volatile
        private var stop = false

        override fun run() {
            while (!stop && held.size < CAP) {
                try {
                    held.add(Os.open("/dev/null", OsConstants.O_RDONLY, 0))
                } catch (e: ErrnoException) {
                    /* EMFILE: enough victims for this round. */
                    break
                }
            }
        }

        /** Stops churning, closes the descriptors that are still ours and returns the rest. */
        fun stopAndRelease(): List<String> {
            stop = true
            join()
            val stolen = ArrayList<String>()
            val seen = HashSet<String>()
            for (fd in held) {
                val key = "fd ${fd.number()}"
                if (!seen.add(key)) {
                    /* The number came back while we were still holding it: somebody closed it. */
                    stolen.add("$key (reused)")
                    continue
                }
                val alive = try {
                    Os.fstat(fd)
                    true
                } catch (e: ErrnoException) {
                    false
                }
                if (alive) {
                    try {
                        Os.close(fd)
                    } catch (e: ErrnoException) {
                        stolen.add("$key (${e.message})")
                    }
                } else {
                    stolen.add("$key (closed)")
                }
            }
            return stolen
        }
    }

    companion object {
        private const val ITERATIONS = 100
        private const val CAP = 20_000

        private fun FileDescriptor.number(): Any = try {
            FileDescriptor::class.java.getMethod("getInt\$").invoke(this) as Int
        } catch (e: Exception) {
            this
        }
    }
}
