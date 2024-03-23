package universe.constellation.orion.viewer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

internal fun OrionBaseActivity.showErrorReportDialog(dialogTitle: String, messageTitle: String, exceptionOrIntentData: String) {
    val view = layoutInflater.inflate(R.layout.crash_dialog, null)
    val textView = view.findViewById<TextView>(R.id.crashTextView)

    textView.text = exceptionOrIntentData

    val header = view.findViewById<TextView>(R.id.crash_message_header)
    header.text = dialogTitle

    val dialog = AlertDialog.Builder(this).setView(view).setTitle(messageTitle).setPositiveButton(R.string.string_send) { dialog, _ ->
        val viaEmail = view.findViewById<RadioButton>(R.id.crash_send_email).isChecked
        try {
            reportErrorVia(viaEmail, messageTitle, exceptionOrIntentData)
        } catch (e: ActivityNotFoundException) {
            showLongMessage("No application can handle this request. Please install ${if (viaEmail) "a web browser" else "an e-mail client"}")
        }

        dialog.dismiss()

    }.setNegativeButton(R.string.string_cancel) { dialog, _ ->
        dialog.dismiss()

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

internal fun OrionBaseActivity.reportErrorVia(viaEmail: Boolean, messageTitle: String, intentOrException: String) {
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