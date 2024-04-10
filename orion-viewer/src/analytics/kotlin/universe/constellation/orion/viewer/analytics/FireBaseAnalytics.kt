package universe.constellation.orion.viewer.analytics

import android.content.Intent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.currentTimeMillis
import universe.constellation.orion.viewer.exceptionStackTrace

class FireBaseAnalytics : Analytics() {

    private lateinit var analytics: FirebaseAnalytics

    private var intentId = 0L

    private var lastTime = System.currentTimeMillis()

    override fun init(): Analytics {
        analytics = Firebase.analytics
        return this
    }

    override fun onNewIntent(intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {
        lastTime = System.currentTimeMillis()
        if (isUserIntent) {
            intentId = lastTime
        }

        logEvent("onNewIntent") {
            param("scheme", intent.scheme)
            param("mime_type", intent.type)
            param("isUserIntent", isUserIntent.toString())
            param("version_name", BuildConfig.VERSION_NAME)
            param("version_code", BuildConfig.VERSION_CODE.toLong())
            param("isNewUI", isNewUI.toString())
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
        logEvent("Exception") {
            param("stacktrace", ex.exceptionStackTrace())
        }
    }

    override fun dialog(name: String, opened: Boolean) {
        logEvent("Dialog") {
            param("name", name)
            param("openedNotClosed", opened.toString())
        }
    }

    private inline fun logEvent(
        event: String,
        crossinline block: ParametersBuilder.() -> Unit
    ) {
        analytics.logEvent(event) {
            block()
            param("id", intentId)
        }
    }
}