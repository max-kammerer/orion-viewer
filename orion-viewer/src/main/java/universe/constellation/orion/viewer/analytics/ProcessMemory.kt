package universe.constellation.orion.viewer.analytics

import android.os.Build
import android.os.Debug
import android.os.Process
import java.io.File

/**
 * The numbers that explain an out-of-memory on a 32-bit device: there the limit is the address
 * space (VmSize against ~3 GB), not the RAM, and a book mapped whole by libdjvu counts against it.
 */
object ProcessMemory {

    val is64Bit: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Process.is64Bit()
    } else {
        System.getProperty("os.arch")?.contains("64") == true
    }

    class Snapshot(val vmSizeMb: Long, val vmPeakMb: Long, val nativeHeapMb: Long) {
        override fun toString() = "vm=${vmSizeMb}M peak=${vmPeakMb}M native=${nativeHeapMb}M 64bit=$is64Bit"
    }

    fun snapshot(): Snapshot {
        val status = readStatus()
        return Snapshot(status["VmSize"] ?: -1, status["VmPeak"] ?: -1, Debug.getNativeHeapAllocatedSize() shr 20)
    }

    /** VmSize and VmPeak from /proc/self/status in MB; the file is always readable by its own process. */
    private fun readStatus(): Map<String, Long> = try {
        File("/proc/self/status").useLines { lines ->
            lines.mapNotNull { line ->
                val key = line.substringBefore(':', "")
                if (key != "VmSize" && key != "VmPeak") return@mapNotNull null
                val kb = line.substringAfter(':').trim().removeSuffix("kB").trim().toLongOrNull() ?: return@mapNotNull null
                key to kb / 1024
            }.toMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }
}
