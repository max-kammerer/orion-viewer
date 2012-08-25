package universe.constellation.orion.viewer.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Device;
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

        if (Device.Info.NOOK_CLASSIC) {
            ImageButton button = (ImageButton) findViewById(R.id.preferences_close);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        PreferenceCategory general = (PreferenceCategory) getPreferenceScreen().findPreference("GENERAL");
        ListPreference screenOrientation = (ListPreference) general.findPreference("screenOrientation");
        if (Device.Info.TWO_SCREEN) {
            general.removePreference(screenOrientation);
        } else {
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
        }

    }

    public void setContentView(int layoutResID) {
        super.setContentView(Device.Info.NOOK_CLASSIC ? R.layout.nook_preferences : layoutResID);
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }

}
