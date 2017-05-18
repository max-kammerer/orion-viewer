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

import android.os.Build
import android.os.Bundle
import android.preference.*

import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceActivity
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.prefs.OrionBookPreferences.Companion.bookPreferences

/**
 * User: mike
 * Date: 02.01.12
 * Time: 17:35
 */
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

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference?): Boolean {
        super.onPreferenceTreeClick(preferenceScreen, preference)
        if (preference != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (preference is PreferenceScreen)
                if (preference.dialog != null)
                    preference.dialog.window!!.decorView.setBackgroundDrawable(this.window.decorView.background.constantState!!.newDrawable())

            if (preference is DialogPreference)
                if (preference.dialog != null)
                    preference.dialog.window!!.decorView.setBackgroundDrawable(this.window.decorView.background.constantState!!.newDrawable())
        }
        return false
    }

    val orionContext: OrionApplication
        get() = applicationContext as OrionApplication

}
