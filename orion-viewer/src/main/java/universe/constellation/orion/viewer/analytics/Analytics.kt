package universe.constellation.orion.viewer.analytics

import android.content.ContentResolver
import android.content.Intent
import android.provider.Settings
import universe.constellation.orion.viewer.BuildConfig
import java.io.File
import java.util.Collections

const val TAP_HELP_DIALOG = "TAPHELPDialog"
const val FALLBACK_DIALOG = "FALLBACK"
const val SHOW_ERROR_DIALOG = "SHOW_ERROR"
const val SHOW_ERROR_PANEL_DIALOG = "SHOW_ERROR_PANEL"

open class Analytics {

    /** [send] false keeps the implementation working but nothing leaves the device. */
    open fun init(send: Boolean = true): Analytics {
        recording = !send
        return this
    }

    @Volatile
    private var recording = false

    private val recorded = Collections.synchronizedList(ArrayList<String>())

    /** Implementations report every event and key here; kept only while [init] ran with collect off. */
    protected fun record(kind: String, name: String, params: Map<String, Any?> = emptyMap()) {
        if (!recording) return
        val rendered = params.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
        recorded.add("$kind $name $rendered")
    }

    /** What the implementation would have sent since the previous call; the tests' window into it. */
    fun drainRecorded(): List<String> = synchronized(recorded) {
        ArrayList(recorded).also { recorded.clear() }
    }

    open fun onNewIntent(contentResolver: ContentResolver, intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {

    }

    /** Called before the engine touches [file], so a crash while opening carries the book context. */
    open fun bookOpening(file: File, memory: ProcessMemory.Snapshot) {

    }

    /**
     * [engine] is the document class, [cacheLimitMb] its engine cache, [vmGrowthMb] how much the
     * address space grew while opening: for djvu that tells whether the book got mapped whole.
     */
    open fun fileOpenedSuccessfully(file: File, engine: String, cacheLimitMb: Long, vmGrowthMb: Long, memory: ProcessMemory.Snapshot) {

    }

    /** An allocation of [requestedBytes] at [place] failed; the error is rethrown by the caller. */
    open fun outOfMemory(place: String, requestedBytes: Long, memory: ProcessMemory.Snapshot) {

    }

    open fun errorDuringInitialFileOpen() {

    }

    open fun error(ex: Throwable, info: String? = null) {

    }

    open fun dialog(name: String, opened: Boolean) {

    }

    open fun onStartStop(activity: String, isStart: Boolean, isNewUser: Boolean) {

    }

    open fun permissionEvent(screen: String, state: Boolean, isNewUser: Boolean) {

    }

    open fun action(name: String) {

    }

    /** A system memory trim reached the open document: [level] is the ComponentCallbacks2 value. */
    open fun memoryTrim(level: Int, keepPercent: Int, engine: String, nativeBeforeMb: Long, nativeAfterMb: Long, vmSizeMb: Long) {

    }

    open fun logWarning(text: String) {

    }

    companion object {

        /**
         * Set by the instrumentation runner before the application starts: the build's real
         * implementation is used with collection off, so its code runs under the tests without
         * reporting anything. Debug builds otherwise get the no-op base class.
         */
        @Volatile
        var dryRun: Boolean = false

        fun initialize(contentResolver: ContentResolver, analytics: Analytics): Analytics {
            if (dryRun) return analytics.init(send = false)
            val isTestLabOrDebug = BuildConfig.DEBUG || Settings.System.getString(contentResolver, "firebase.test.lab").toBoolean()
            if (!isTestLabOrDebug) {
                return analytics.init()
            }
            return Analytics().init()
        }
    }
}