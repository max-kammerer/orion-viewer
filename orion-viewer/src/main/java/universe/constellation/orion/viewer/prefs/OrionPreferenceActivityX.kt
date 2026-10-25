package universe.constellation.orion.viewer.prefs

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.device.EInkDevice

class OrionPreferenceActivityX : OrionBaseActivity() {
    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.activity_with_fragment, true, true)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, OrionPreferenceFragmentX())
                .commit()
        }
    }
}

class OrionPreferenceFragmentX : DSLPreferenceFragment()
{

    private val settingsTransfer = SettingsTransferUi(this, this) { activity as? OrionBaseActivity }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.userpreferences, rootKey)

        val screen = preferenceScreen
        if (orionApplication.device !is EInkDevice) {
            screen.findPreference<androidx.preference.PreferenceCategory>("NOOK2_EINK")?.let {
                screen.removePreference(it)
            }
        }

        if (!settingsTransfer.isAvailable) {
            screen.findPreference<PreferenceCategory>("SETTINGS_TRANSFER")?.let { screen.removePreference(it) }
        } else {
            screen.findPreference<Preference>("EXPORT_SETTINGS")?.setOnPreferenceClickListener {
                settingsTransfer.export()
                true
            }
            screen.findPreference<Preference>("IMPORT_SETTINGS")?.setOnPreferenceClickListener {
                settingsTransfer.import()
                true
            }
        }
    }

    private val orionApplication: OrionApplication
        get() = requireContext().applicationContext as OrionApplication
}

class BehaviourPreferenceFragment : SwitchHeaderPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_pref_controls, rootKey)
    }
}

class AppearancePreferenceFragment : SwitchHeaderPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
         setPreferencesFromResource(R.xml.user_pref_appearance, rootKey)
    }
}

abstract class SwitchHeaderPreferenceFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = preferenceScreen.title
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        val summary = preferenceScreen.summary
        if (title != summary) {
            (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = summary
        }
    }
}