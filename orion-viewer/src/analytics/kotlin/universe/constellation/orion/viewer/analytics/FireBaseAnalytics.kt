package universe.constellation.orion.viewer.analytics

import android.content.ContentResolver
import android.content.Intent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.currentTimeMillis
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase
import universe.constellation.orion.viewer.filemanager.fileExtension
import universe.constellation.orion.viewer.formats.FileFormats.Companion.getFileExtFromPath
import universe.constellation.orion.viewer.formats.FileFormats.Companion.getMimeType
import universe.constellation.orion.viewer.formats.FileFormats.Companion.isSupportedMimeType
import java.io.File


class FireBaseAnalytics : Analytics() {

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics

    private var lastTime = System.currentTimeMillis()

    override fun init(collect: Boolean): Analytics {
        super.init(collect)
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(collect)
        crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(collect)
        return this
    }

    override fun onNewIntent(contentResolver: ContentResolver, intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {
        lastTime = System.currentTimeMillis()

        logEvent("onNewIntent") {
            param("scheme", intent.scheme)
            val mimeType = contentResolver.getMimeType(intent)
            param("mime_type", mimeType)
            param("isUserIntent", isUserIntent.toString())
            param("version_code", BuildConfig.VERSION_CODE.toLong())
            param("isNewUI", isNewUI.toString())
            param("isSystemFM", intent.getBooleanExtra(OrionFileManagerActivityBase.SYSTEM_FILE_MANAGER, false).toString())
            if (!mimeType.isSupportedMimeType) {
                param("book_ext", intent.getFileExtFromPath())
                param("host", intent.data?.host)
            }
        }
    }

    private fun ParametersBuilder.param(key: String, value: String?) {
        param(key, value ?: "<null>")
    }

    override fun bookOpening(file: File, memory: ProcessMemory.Snapshot) {
        key("process_64bit", ProcessMemory.is64Bit)
        key("book_size_mb", file.length() shr 20)
        key("book_ext", file.name.fileExtension)
        key("engine", "opening")
        memoryKeys(memory)
    }

    override fun fileOpenedSuccessfully(file: File, engine: String, cacheLimitMb: Long, vmGrowthMb: Long, memory: ProcessMemory.Snapshot) {
        key("engine", engine)
        key("cache_limit_mb", cacheLimitMb)
        key("book_vm_growth_mb", vmGrowthMb)
        memoryKeys(memory)
        fileInfo(true) {
            val hash = file.length().hashCode() + file.name.hashCode()
            param("book_id", hash.toLong())
            param("book_ext", file.name.fileExtension)
            param("file_size", file.length())
            param("engine", engine)
            param("cache_limit_mb", cacheLimitMb)
            param("vm_growth_mb", vmGrowthMb)
            param("process_64bit", ProcessMemory.is64Bit.toString())
            param("vm_size_mb", memory.vmSizeMb)
            param("native_heap_mb", memory.nativeHeapMb)
        }
    }

    override fun outOfMemory(place: String, requestedBytes: Long, memory: ProcessMemory.Snapshot) {
        memoryKeys(memory)
        logEvent("oom") {
            param("place", place)
            param("requested_mb", requestedBytes shr 20)
            param("process_64bit", ProcessMemory.is64Bit.toString())
            param("vm_size_mb", memory.vmSizeMb)
            param("vm_peak_mb", memory.vmPeakMb)
            param("native_heap_mb", memory.nativeHeapMb)
        }
        crashlytics.log("OOM at $place, requested ${requestedBytes shr 20}M, $memory")
    }

    private fun memoryKeys(memory: ProcessMemory.Snapshot) {
        key("vm_size_mb", memory.vmSizeMb)
        key("vm_peak_mb", memory.vmPeakMb)
        key("native_heap_mb", memory.nativeHeapMb)
    }

    override fun errorDuringInitialFileOpen() {
        fileInfo(false)
    }

    private inline fun fileInfo(successful: Boolean, crossinline block: ParametersBuilder.() -> Unit = {}) {
        logEvent("fileOpened") {
            param("time", currentTimeMillis() - lastTime)
            param("state", successful.toString())
            block()
        }
    }

    override fun error(ex: Throwable, info: String?) {
        info?.let { crashlytics.log(it) }
        crashlytics.recordException(ex)
    }

    override fun logWarning(text: String) {
        crashlytics.log(text)
    }

    override fun dialog(name: String, opened: Boolean) {
        logEvent("Dialog") {
            param("name", name)
            param("openedNotClosed", opened.toString())
        }
    }

    override fun onStartStop(activity: String, isStart: Boolean, isNewUser: Boolean) {
        logEvent("onStartStop") {
            param("isNewUser", isNewUser.toString())
            param("isStart", isStart.toString())
        }
    }

    override fun permissionEvent(screen: String, state: Boolean, isNewUser: Boolean) {
        logEvent("permissionResult") {
            param("screen", screen)
            param("state", state.toString())
            param("isNewUser", isNewUser.toString())
        }
    }

    override fun action(name: String) {
        logEvent("action") {
            param("name", name)
        }
    }

    override fun memoryTrim(level: Int, keepPercent: Int, engine: String, nativeBeforeMb: Long, nativeAfterMb: Long, vmSizeMb: Long) {
        key("vm_size_mb", vmSizeMb)
        key("native_heap_mb", nativeAfterMb)
        logEvent("memoryTrim") {
            param("level", level.toLong())
            param("keepPercent", keepPercent.toLong())
            param("engine", engine)
            param("nativeBeforeMb", nativeBeforeMb)
            param("nativeAfterMb", nativeAfterMb)
            param("vmSizeMb", vmSizeMb)
        }
    }

    private inline fun logEvent(
        event: String,
        crossinline block: ParametersBuilder.() -> Unit = {}
    ) {
        val params = ParametersBuilder().apply(block)
        analytics.logEvent(event, params.bundle)
        record("event", event, params.bundle.keySet().associateWith { params.bundle.get(it) })
    }

    private fun key(name: String, value: Any) {
        when (value) {
            is Boolean -> crashlytics.setCustomKey(name, value)
            is Long -> crashlytics.setCustomKey(name, value)
            is Int -> crashlytics.setCustomKey(name, value)
            else -> crashlytics.setCustomKey(name, value.toString())
        }
        record("key", name, mapOf("value" to value))
    }
}