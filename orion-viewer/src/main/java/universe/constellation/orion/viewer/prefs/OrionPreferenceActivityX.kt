package universe.constellation.orion.viewer.prefs

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.device.EInkDevice

class OrionPreferenceActivityX : OrionBaseActivity(false) {
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.userpreferences, rootKey)

        val screen = preferenceScreen
        if (orionApplication.device !is EInkDevice) {
            screen.findPreference<androidx.preference.PreferenceCategory>("NOOK2_EINK")?.let {
                screen.removePreference(it)
            }
        }
    }

    private val orionApplication: OrionApplication
        get() = requireContext().applicationContext as OrionApplication
}

class BehaviourPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_pref_general, rootKey)
    }
}

class AppearancePreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
         setPreferencesFromResource(R.xml.user_pref_appearance, rootKey)
    }
}