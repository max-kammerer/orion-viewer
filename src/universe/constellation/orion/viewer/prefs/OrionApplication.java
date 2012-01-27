package universe.constellation.orion.viewer.prefs;

import android.app.Activity;
import android.app.Application;
import pl.polidea.demo.R;

/**
 * User: mike
 * Date: 23.01.12
 * Time: 20:03
 */
public class OrionApplication extends Application {

    private GlobalOptions options;

    private TemporaryOptions tempOptions;

    public GlobalOptions getOptions() {
        if (options == null) {
            options = new GlobalOptions(this);
        }
        return options;
    }

    public void onNewBook() {
        tempOptions = new TemporaryOptions();
    }

    public TemporaryOptions getTempOptions() {
        return tempOptions;
    }


    public void applyTheme(Activity activity) {
        String theme = getOptions().getApplicationTheme();
        int themeId = android.R.style.Theme_NoTitleBar;
        if ("DARK".equals(theme)) {
            themeId = android.R.style.Theme_Black_NoTitleBar;
        } else if ("LIGHT".equals(theme)) {
            themeId = android.R.style.Theme_Light_NoTitleBar;
        }
        activity.setTheme(themeId);
    }
}
