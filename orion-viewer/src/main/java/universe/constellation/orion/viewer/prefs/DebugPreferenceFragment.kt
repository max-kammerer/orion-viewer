package universe.constellation.orion.viewer.prefs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import universe.constellation.orion.viewer.FallbackDialogs.Companion.saveFileByUri
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat

class DebugPreferenceFragment : SwitchHeaderPreferenceFragment() {

    private val REQUEST_CODE = 1000

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preference, rootKey)

        val debugLogFolder =
            (requireContext().applicationContext as OrionApplication).debugLogFolder() ?: return

        val files = debugLogFolder.listFiles() ?: return
        if (files.isNotEmpty()) {
            val logs =
                preferenceScreen.findPreference<PreferenceCategory>("LOGS") ?: return
            logs.summary = null
            files.forEachIndexed { index, file ->
                logs.addPreference(
                    Preference(requireContext()).apply {
                        isPersistent = false
                        title = file.name
                        isIconSpaceReserved = false
                        onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                Intent().apply {
                                    type = "text/plain"
                                    if (isAtLeastKitkat()) {
                                        action = Intent.ACTION_CREATE_DOCUMENT
                                        startActivityForResult(this, REQUEST_CODE + index)
                                    } else {
                                        action = Intent.ACTION_SEND
                                        val uri = Uri.fromFile(file)
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        startActivity(this)
                                    }
                                }
                                true
                            }
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val fileIndex = requestCode - REQUEST_CODE
            val debugLogFolder =
                (requireContext().applicationContext as OrionApplication).debugLogFolder() ?: return
            val file = debugLogFolder.listFiles()?.getOrNull(fileIndex) ?: return
            val intent = requireActivity().intent
            (requireActivity() as OrionBaseActivity).saveFileByUri(intent, Uri.fromFile(file), data.data ?: return) {}

        }
    }
}