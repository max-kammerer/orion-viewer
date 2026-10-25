package universe.constellation.orion.viewer.prefs

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.log
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The export and import of settings for a screen ([SettingsTransfer]): the settings screen and the
 * file manager's menu. Create it as a field, the system file picker is registered on creation;
 * [activity] gives the screen's activity when it's needed, for the confirmation dialog.
 */
class SettingsTransferUi(
    caller: ActivityResultCaller,
    private val owner: LifecycleOwner,
    private val activity: () -> OrionBaseActivity?,
) {

    /* The system file picker, Android 4.4+: older devices don't get the transfer items. */
    private val exportLauncher = if (isAtLeastKitkat()) caller.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(::exportTo) } else null

    private val importLauncher = if (isAtLeastKitkat()) caller.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::confirmImportFrom) } else null

    val isAvailable: Boolean
        get() = exportLauncher != null && importLauncher != null

    fun export() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        exportLauncher?.launch("orion_viewer_settings_$date.json")
    }

    fun import() {
        //some providers report json as text or as unknown binary
        importLauncher?.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
    }

    private val app: OrionApplication?
        get() = activity()?.applicationContext as? OrionApplication

    private fun exportTo(uri: Uri) {
        val app = app ?: return
        val json = SettingsTransfer.export(app.options.prefs, app.keyBindingPrefs.prefs)
        transfer(app, R.string.settings_exported) {
            app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("No stream for $uri")
        }
    }

    private fun confirmImportFrom(uri: Uri) {
        (activity() ?: return).createThemedAlertBuilder()
            .setMessage(R.string.settings_import_confirm)
            .setPositiveButton(R.string.pref_import_settings) { _, _ -> importFrom(uri) }
            .setNegativeButton(R.string.string_cancel, null)
            .show()
    }

    private fun importFrom(uri: Uri) {
        val app = app ?: return
        transfer(app, R.string.settings_imported) {
            val json = app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IOException("No stream for $uri")
            //on the main thread: the options' listener updates the open app as the values change
            withContext(Dispatchers.Main) {
                SettingsTransfer.import(json, app.options.prefs, app.keyBindingPrefs.prefs)
            }
        }
    }

    /** Runs the file work off the main thread and tells how it went. */
    private fun transfer(app: OrionApplication, doneMessage: Int, work: suspend () -> Unit) {
        owner.lifecycleScope.launch {
            val message = try {
                withContext(Dispatchers.IO) { work() }
                app.getString(doneMessage)
            } catch (e: IllegalArgumentException) {
                log("Settings transfer: ${e.message}")
                app.getString(R.string.settings_import_failed)
            } catch (e: Exception) {
                log("Settings transfer failed", e)
                app.getString(R.string.settings_transfer_failed, e.message ?: e.javaClass.simpleName)
            }
            Toast.makeText(app, message, Toast.LENGTH_LONG).show()
        }
    }
}
