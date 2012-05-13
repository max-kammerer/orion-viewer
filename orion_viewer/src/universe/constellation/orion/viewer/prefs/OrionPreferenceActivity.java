package universe.constellation.orion.viewer.prefs;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
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

import android.content.Context;
import android.os.Bundle;
import android.preference.*;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.Device;
import com.google.code.orion_viewer.device.AndroidDevice;
import com.google.code.orion_viewer.device.NookDevice;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 02.01.12
 * Time: 17:35
 */
public class OrionPreferenceActivity extends PreferenceActivity {

    private boolean isNook;

    private boolean isAndroidGeneral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);
        Device device = Common.createDevice();
        isNook = device instanceof NookDevice;
        isAndroidGeneral = device instanceof AndroidDevice && !isNook;

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
        if (isNook) {
            ImageButton button = (ImageButton) findViewById(R.id.preferences_close);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        PreferenceScreen screen = getPreferenceScreen();

        screen.findPreference("EINK_OPTIMIZATION").setEnabled(Device.Info.NOOK2);

        PreferenceCategory LAYOUT = (PreferenceCategory) screen.findPreference("LAYOUT");
        ListPreference SCREEN_ORIENTATION = (ListPreference) findPreference("SCREEN_ORIENTATION");
        Preference BOOK_ORIENTATION = screen.findPreference("BOOK_ORIENTATION");

        if (!Device.Info.TWO_SCREEN) {
            LAYOUT.removePreference(BOOK_ORIENTATION);
        }

        if (!isAndroidGeneral) {
            LAYOUT.removePreference(SCREEN_ORIENTATION);
        } else {
            if (getOrionContext().getSdkVersion() >= 9) {
                SCREEN_ORIENTATION.setEntries(getResources().getTextArray(R.array.screen_orientation_full));
                SCREEN_ORIENTATION.setEntryValues(getResources().getTextArray(R.array.screen_orientation_full_desc));
            }
        }
    }


    @Override
    public void addPreferencesFromResource(int preferencesResId) {

        super.addPreferencesFromResource(preferencesResId);
    }



    public void setContentView(int layoutResID) {
        super.setContentView(isNook ? R.layout.nook_preferences : layoutResID);
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }
}
