package universe.constellation.orion.viewer.test

import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.analytics.ProcessMemory
import universe.constellation.orion.viewer.test.framework.BaseTest

class ProcessMemoryTest : BaseTest() {
    @Test
    fun snapshotReadsTheProcessStatus() {
        val snapshot = ProcessMemory.snapshot()
        assertTrue("$snapshot", snapshot.vmSizeMb > 0)
        assertTrue("$snapshot", snapshot.vmPeakMb >= snapshot.vmSizeMb)
        assertTrue("$snapshot", snapshot.nativeHeapMb > 0)
    }
}
