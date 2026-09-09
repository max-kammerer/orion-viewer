package universe.constellation.orion.viewer.test

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.analytics.Analytics
import universe.constellation.orion.viewer.analytics.ProcessMemory
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription

/* Under the tests the application gets the implementation the build was made with (Firebase when
 * google-services.json is present, the no-op base otherwise) in dry-run mode, so every event and
 * Crashlytics key is exercised without leaving the device. A wrong parameter type or an
 * uninitialised field would fail here instead of in release. */
class AnalyticsTest : BaseTest() {

    private val application
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication

    @Test
    fun testsRunTheBuildsImplementation() {
        assertEquals(BuildConfig.ANALYTICS.javaClass, application.analytics.javaClass)
    }

    @Test
    fun everyEventAcceptsItsParameters() {
        val analytics = application.analytics
        val book = BookDescription.SICP.asFile()
        val memory = ProcessMemory.snapshot()
        analytics.bookOpening(book, memory)
        analytics.fileOpenedSuccessfully(book, "PdfDocument", 64, 0, memory)
        analytics.errorDuringInitialFileOpen()
        analytics.memoryTrim(40, 25, "DjvuDocument", memory.nativeHeapMb, memory.nativeHeapMb, memory.vmSizeMb)
        analytics.outOfMemory("test", 128L shl 20, memory)
        analytics.dialog("test", true)
        analytics.action("test")
        analytics.onStartStop("test", isStart = true, isNewUser = false)
        analytics.permissionEvent("test", state = true, isNewUser = false)
        analytics.logWarning("test")
        analytics.error(RuntimeException("test"), "test")

        if (analytics.javaClass == Analytics::class.java) return
        val recorded = analytics.drainRecorded()
        val expected = listOf(
            "event fileOpened ", "event memoryTrim ", "event oom ", "event Dialog ", "event action ",
            "key process_64bit ", "key book_size_mb ", "key engine ", "key vm_size_mb ", "key cache_limit_mb ", "key book_vm_growth_mb "
        )
        expected.forEach { prefix ->
            assertTrue("$prefix missing in:\n${recorded.joinToString("\n")}", recorded.any { it.startsWith(prefix) })
        }
        val fileOpened = recorded.first { it.startsWith("event fileOpened ") }
        listOf("engine=PdfDocument", "cache_limit_mb=64", "vm_size_mb=${memory.vmSizeMb}", "process_64bit=${ProcessMemory.is64Bit}").forEach {
            assertTrue("$it missing in $fileOpened", it in fileOpened)
        }
    }

    /* A real book open through the activity ends with the fileOpened event carrying the memory context. */
    @Test
    fun openingABookReportsItsContext() {
        val analytics = application.analytics
        if (analytics.javaClass == Analytics::class.java) return
        analytics.drainRecorded()
        ActivityScenario.launch<OrionViewerActivity>(BookDescription.SICP.toOpenIntent()).use {
            Espresso.onIdle()
            val recorded = analytics.drainRecorded()
            val opening = recorded.filter { it.startsWith("key engine ") }
            assertTrue("engine keys: $opening", opening.any { "value=opening" in it } && opening.any { "value=PdfDocument" in it })
            assertTrue("fileOpened: $recorded", recorded.any { it.startsWith("event fileOpened ") && "book_ext=pdf" in it && "vm_growth_mb=" in it })
        }
    }
}
