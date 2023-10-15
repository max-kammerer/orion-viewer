/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.prefs

import android.os.Bundle
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import androidx.appcompat.app.AppCompatActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceActivity
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.prefs.OrionBookPreferences.Companion.bookPreferences
import universe.constellation.orion.viewer.prefs.OrionBookPreferencesFragment.Companion.bookPreferences

class OrionPreferenceActivity : DSLPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        orionContext.applyTheme(this)

        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.userpreferences)

        val screen = preferenceScreen

        val eInkDevice = screen.findPreference("NOOK2_EINK") as PreferenceCategory

        if (orionContext.device !is EInkDevice) {
            screen.removePreference(eInkDevice)
        }

        val bookPreferences = screen.findPreference("BOOK_DEFAULT") as PreferenceScreen
        bookPreferences.nested {
            bookPreferences(this@nested, true)
        }
    }

    private val orionContext: OrionApplication
        get() = applicationContext as OrionApplication
}

class OrionPreferenceActivityX : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as OrionApplication).applyTheme(this)
        setContentView(R.layout.activity_with_fragment)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, OrionPreferenceFragmentX())
            .commit()

    }
}

class OrionPreferenceFragmentX : DSLPreferenceFragment() {
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
