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
import android.preference.*;
import android.view.View;
import android.widget.ImageButton;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Device;
import universe.constellation.orion.viewer.device.AndroidDevice;
import universe.constellation.orion.viewer.device.NookDevice;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 02.01.12
 * Time: 17:35
 */
public class OrionPreferenceActivity extends PreferenceActivity {

    private boolean isAndroidGeneral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);

        isAndroidGeneral = !Device.Info.TWO_SCREEN;

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
        if (Device.Info.NOOK_CLASSIC) {
            ImageButton button = (ImageButton) findViewById(R.id.preferences_close);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        PreferenceScreen screen = getPreferenceScreen();

        PreferenceCategory NOOK2_EINK = (PreferenceCategory) screen.findPreference("NOOK2_EINK");

        if (!Device.Info.NOOK2) {
            screen.removePreference(NOOK2_EINK);
        }

        PreferenceCategory GENERAL = (PreferenceCategory) screen.findPreference("GENERAL");
        ListPreference SCREEN_ORIENTATION = (ListPreference) findPreference("SCREEN_ORIENTATION");

        PreferenceScreen BOOK_DEFAULT = (PreferenceScreen) screen.findPreference("BOOK_DEFAULT");
        Preference BOOK_ORIENTATION = BOOK_DEFAULT.findPreference("BOOK_ORIENTATION");

        if (!isAndroidGeneral) {
            GENERAL.removePreference(SCREEN_ORIENTATION);
        } else {
            BOOK_DEFAULT.removePreference(BOOK_ORIENTATION);

            if (getOrionContext().getSdkVersion() >= 9) {
                SCREEN_ORIENTATION.setEntries(getResources().getTextArray(R.array.screen_orientation_full));
                SCREEN_ORIENTATION.setEntryValues(getResources().getTextArray(R.array.screen_orientation_full_desc));
            }
        }

        if (Device.Info.NOOK_CLASSIC) {
            //nook classic hacks
            final PreferenceScreen bookDefaults = (PreferenceScreen) GENERAL.findPreference("BOOK_DEFAULT");
            PreferenceScreen taps = (PreferenceScreen) GENERAL.findPreference("TAP_ZONES");
            GENERAL.removePreference(taps);

            //nook doesn't support inner preference screen
            PreferenceCategory newBookDefaults = new PreferenceCategory(getApplicationContext(), null);
            newBookDefaults.setTitle("Book default options");
            screen.addPreference(newBookDefaults);

            for (int i = bookDefaults.getPreferenceCount() - 1; i >= 0; i--) {
                Preference pref = bookDefaults.getPreference(i);
                bookDefaults.removePreference(pref);
                newBookDefaults.addPreference(pref);
            }
            GENERAL.removePreference(bookDefaults);
        }

    }






    public void setContentView(int layoutResID) {
        super.setContentView(Device.Info.NOOK_CLASSIC ? R.layout.nook_preferences : layoutResID);
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }


}
