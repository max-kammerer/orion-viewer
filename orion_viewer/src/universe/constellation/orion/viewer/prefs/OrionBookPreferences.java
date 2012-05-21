package universe.constellation.orion.viewer.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.view.View;
import android.widget.ImageButton;
import universe.constellation.orion.viewer.Common;
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

    }

    public void setContentView(int layoutResID) {
        super.setContentView(Device.Info.NOOK_CLASSIC ? R.layout.nook_preferences : layoutResID);
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }

}
