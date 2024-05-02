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

    private var lastTime = System.currentTimeMillis()

    override fun init(): Analytics {
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        return this
    }

    override fun onNewIntent(contentResolver: ContentResolver, intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {
        lastTime = System.currentTimeMillis()

        logEvent("onNewIntent") {
            param("scheme", intent.scheme)
            val mimeType = contentResolver.getMimeType(intent)
            param("mime_type", mimeType)
            param("book_ext", intent.getFileExtFromPath())
            param("isUserIntent", isUserIntent.toString())
            param("version_code", BuildConfig.VERSION_CODE.toLong())
            param("isNewUI", isNewUI.toString())
            param("isSystemFM", intent.getBooleanExtra(OrionFileManagerActivityBase.SYSTEM_FILE_MANAGER, false).toString())
            if (!mimeType.isSupportedMimeType) {
                param("host", intent.data?.host)
            }
        }
    }

    private fun ParametersBuilder.param(key: String, value: String?) {
        param(key, value ?: "<null>")
    }

    override fun fileOpenedSuccessfully(file: File) {
        fileInfo(true) {
            val hash = (file.length().hashCode().toLong() shl 32) + file.name.hashCode()
            param("book_id", hash)
            param("book_ext", file.name.fileExtension)
        }
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

    override fun error(ex: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(ex)
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

    private inline fun logEvent(
        event: String,
        crossinline block: ParametersBuilder.() -> Unit = {}
    ) {
        analytics.logEvent(event) {
            block()
        }
    }
}