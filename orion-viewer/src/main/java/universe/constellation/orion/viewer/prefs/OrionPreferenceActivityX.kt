package universe.constellation.orion.viewer.prefs

import android.annotation.SuppressLint
import android.os.Bundle
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.prefs.OrionBookPreferencesFragment.Companion.bookPreferences

class OrionPreferenceActivityX : OrionBaseActivity(false) {
    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.activity_with_fragment, true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, OrionPreferenceFragmentX())
            .commit()
    }
}

class OrionPreferenceFragmentX : DSLPreferenceFragment()
{

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.userpreferences, rootKey)

        val screen = preferenceScreen

        if (orionContext.device !is EInkDevice) {
            screen.findPreference<androidx.preference.PreferenceCategory>("NOOK2_EINK")?.let {
                screen.removePreference(it)
            }
        }

        val bookPreferences = screen.findPreference<androidx.preference.PreferenceScreen>("BOOK_DEFAULT")!!
        bookPreferences.nested {
            bookPreferences(this@nested, true)
        }
    }

    private val orionContext: OrionApplication
        get() = context as OrionApplication
}