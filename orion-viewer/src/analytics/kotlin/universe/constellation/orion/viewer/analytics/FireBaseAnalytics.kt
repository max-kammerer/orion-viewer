package universe.constellation.orion.viewer.analytics

import android.content.ContentResolver
import android.content.Intent
import android.webkit.MimeTypeMap
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.currentTimeMillis
import universe.constellation.orion.viewer.filemanager.fileExtension
import java.io.File


class FireBaseAnalytics : Analytics() {

    private lateinit var analytics: FirebaseAnalytics

    private var intentId = 0L

    private var lastTime = System.currentTimeMillis()

    override fun init(): Analytics {
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        return this
    }

    override fun onNewIntent(contentResolver: ContentResolver, intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {
        lastTime = System.currentTimeMillis()
        if (isUserIntent) {
            intentId = lastTime
        }

        logEvent("onNewIntent") {
            param("scheme", intent.scheme)
            param("mime_type", contentResolver.getMimeType(intent))
            param("isUserIntent", isUserIntent.toString())
            param("version_code", BuildConfig.VERSION_CODE.toLong())
            param("isNewUI", isNewUI.toString())
        }
    }

    private fun ContentResolver.getMimeType(intent: Intent): String? {
        val type = intent.type
        if (type != null) return type
        val uri = intent.data ?: return "<intent/null_data>"
        when (val scheme = intent.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                return getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                        ?: return "<content/no_extension>"
                )
            }
            ContentResolver.SCHEME_FILE -> {
                val file = File(uri.path ?: return  "<file/no_path>")
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.name.fileExtension.takeIf { it.isNotBlank() } ?: return "<file/no_extension>")
            }
            else -> {
                return "<unknown_scheme/$scheme>"
            }
        }
    }

    private fun ParametersBuilder.param(key: String, value: String?) {
        param(key, value ?: "<null>")
    }

    override fun fileOpenedSuccessfully() {
        fileInfo(true)
    }

    override fun errorDuringInitialFileOpen() {
        fileInfo(false)
    }

    private fun fileInfo(successful: Boolean) {
        logEvent("fileOpened") {
            param("time", currentTimeMillis() - lastTime)
            param("state", successful.toString())
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

    override fun onApplicationInit() {
        logEvent("onApplicationInit")
    }

    private inline fun logEvent(
        event: String,
        crossinline block: ParametersBuilder.() -> Unit = {}
    ) {
        analytics.logEvent(event) {
            block()
            param("id", intentId)
        }
    }
}