package universe.constellation.orion.viewer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import universe.constellation.orion.viewer.analytics.SHOW_ERROR_DIALOG
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.RuntimeException

internal fun Throwable.exceptionStackTrace(): String {
    val exceptionWriter = StringWriter()
    val printWriter = PrintWriter(exceptionWriter)
    printStackTrace(printWriter)
    printWriter.flush()
    return exceptionWriter.toString()
}

internal fun OrionBaseActivity.showErrorReportDialog(dialogTitle: Int, messageTitle: Int, intent: Intent, info: String, exception: Throwable) {
    showErrorReportDialog(resources.getString(dialogTitle), resources.getString(messageTitle), intent, info, exception)
}

internal fun OrionBaseActivity.showErrorReportDialog(dialogTitle: String, messageTitle: String, intent: Intent, info: String? = null, exception: Throwable? = null) {
    this.analytics.error(exception ?: RuntimeException(), intent.toString())

    val view = layoutInflater.inflate(R.layout.crash_dialog, null)
    val textView = view.findViewById<TextView>(R.id.crashTextView)

    val fullMessage = prepareFullErrorMessage(intent, info, exception)

    textView.text = fullMessage

    val header = view.findViewById<TextView>(R.id.crash_message_header)
    header.text = dialogTitle

    analytics.dialog(SHOW_ERROR_DIALOG, true)
    val dialog = AlertDialog.Builder(this).setView(view).setTitle(messageTitle).setPositiveButton(R.string.string_send) { dialog, _ ->
        val viaEmail = view.findViewById<RadioButton>(R.id.crash_send_email).isChecked
        try {
            reportErrorVia(viaEmail, messageTitle, fullMessage)
        } catch (e: ActivityNotFoundException) {
            this.analytics.error(e)
            showLongMessage("No application can handle this request. Please install ${if (viaEmail) "a web browser" else "an e-mail client"}")
        }

        dialog.dismiss()
    }.setNegativeButton(R.string.string_cancel) { dialog, _ ->
        dialog.dismiss()
    }.setOnDismissListener {
        analytics.dialog(SHOW_ERROR_DIALOG, false)
    }.create()

    dialog.show()

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

    view.findViewById<RadioButton>(R.id.crash_send_email).setOnClickListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
    }
    view.findViewById<RadioButton>(R.id.crash_send_github).setOnClickListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
    }
}

internal fun prepareFullErrorMessage(
    intent: Intent,
    info: String?,
    exception: Throwable?,
    addIntent: Boolean = true,
    addStackTrace: Boolean = true
): String {
    return (("Cause: ${exception?.message}\n\n".takeIf { exception?.message != null } ?: "") +
            (("Additional info: $info\n\n").takeIf { info != null } ?: "") +
            (if (addIntent) "Intent info: $intent" else "") +
            if (addStackTrace) (exception?.let { "\n\n" + it.exceptionStackTrace() } ?: "") else "").trim()
}

internal fun Activity.reportErrorVia(viaEmail: Boolean, messageTitle: String, intentOrException: String) {
    val bodyWithException =
        """
            ${applicationContext.getString(R.string.send_report_header)}

            Orion Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})}
            Android Version: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})
            ${if (viaEmail) "" else "```"}
            $intentOrException
            ${if (viaEmail) "" else "```"}
        """.trimIndent()

    if (viaEmail) {
        val intent = Intent(Intent.ACTION_SENDTO)
        val email = "mikhael" + "." + "bogdanov" + "+" + "orion" + "@" + "gmail" + "." + "com"
        intent.data = Uri.Builder().scheme("mailto").opaquePart(email)
            .appendQueryParameter("subject", "Orion Viewer: $messageTitle")
            .appendQueryParameter("body", bodyWithException).build()

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Orion Viewer: $messageTitle")
        intent.putExtra(Intent.EXTRA_TEXT, bodyWithException)

        startActivity(Intent.createChooser(intent, "Send Email..."))
    } else {
        val myIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/max-kammerer/orion-viewer/issues/new").buildUpon().appendQueryParameter("title", messageTitle).appendQueryParameter("body", bodyWithException).build()
        )
        startActivity(myIntent)
    }
}