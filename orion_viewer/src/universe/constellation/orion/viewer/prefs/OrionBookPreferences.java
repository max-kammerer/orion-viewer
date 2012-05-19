package universe.constellation.orion.viewer.prefs;

import android.os.Bundle;
import android.preference.*;
import android.view.View;
import android.widget.ImageButton;
import universe.constellation.orion.viewer.Device;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 17.05.12
 * Time: 21:41
 */
public class OrionBookPreferences extends PreferenceActivity {

    private boolean isAndroidGeneral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);

        isAndroidGeneral = !Device.Info.TWO_SCREEN;

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

        PreferenceScreen screen = getPreferenceScreen();

        screen.findPreference("EINK_OPTIMIZATION").setEnabled(Device.Info.NOOK2);

        PreferenceCategory LAYOUT = (PreferenceCategory) screen.findPreference("LAYOUT");
        ListPreference SCREEN_ORIENTATION = (ListPreference) findPreference("SCREEN_ORIENTATION");
        Preference BOOK_ORIENTATION = screen.findPreference("BOOK_ORIENTATION");


        if (!isAndroidGeneral) {
            LAYOUT.removePreference(SCREEN_ORIENTATION);
        } else {
            LAYOUT.removePreference(BOOK_ORIENTATION);

            if (getOrionContext().getSdkVersion() >= 9) {
                SCREEN_ORIENTATION.setEntries(getResources().getTextArray(R.array.screen_orientation_full));
                SCREEN_ORIENTATION.setEntryValues(getResources().getTextArray(R.array.screen_orientation_full_desc));
            }
        }
    }


    public void setContentView(int layoutResID) {
        super.setContentView(Device.Info.NOOK_CLASSIC ? R.layout.nook_preferences : layoutResID);
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
    }
}
