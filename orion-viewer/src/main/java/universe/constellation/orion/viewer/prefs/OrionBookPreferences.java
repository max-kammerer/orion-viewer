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

package universe.constellation.orion.viewer.prefs;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 17.05.12
 * Time: 21:41
 */
public class OrionBookPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.book_preference);

        PreferenceCategory general = (PreferenceCategory) getPreferenceScreen().findPreference("GENERAL");
        ListPreference screenOrientation = (ListPreference) general.findPreference("screenOrientation");

        boolean isLevel9 = getOrionContext().getSdkVersion() >= 9;


        CharSequence[] values = getResources().getTextArray(isLevel9 ? R.array.screen_orientation_full_desc : R.array.screen_orientation_desc);
        CharSequence[] newValues = new CharSequence[values.length];
        for (int i = 0; i < values.length; i++) {
            newValues[i] = values[i];
        }
        newValues[0] = getResources().getString(R.string.orientation_default_rotation);
        screenOrientation.setEntries(newValues);

        if (isLevel9) {
            screenOrientation.setEntryValues(R.array.screen_orientation_full);
        }
//            if (!isLevel9) {
//                CharSequence [] entries = screenOrientation.getEntries();
//                CharSequence [] values = screenOrientation.getEntryValues();
//                entries = Arrays.copyOf(entries, 3);
//                values = Arrays.copyOf(values, 3);
//                screenOrientation.setEntries(entries);
//                screenOrientation.setEntryValues(values);
//            }

    }


    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }

}
