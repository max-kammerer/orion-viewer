package universe.constellation.orion.viewer.prefs;

import android.app.Activity;
import android.app.Application;
import universe.constellation.orion.viewer.db.BookmarkAccessor;

/**
 * User: mike
 * Date: 23.01.12
 * Time: 20:03
 */
public class OrionApplication extends Application {

    private GlobalOptions options;

    private TemporaryOptions tempOptions;

    public static OrionApplication instance;

    private BookmarkAccessor bookmarkAccessor;


    public void onCreate() {
        instance = this;
        super.onCreate();    //To change body of overridden methods use File | Settings | File Templates.
    }

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

    public BookmarkAccessor getBookmarkAccessor() {
        if (bookmarkAccessor == null) {
            bookmarkAccessor = new BookmarkAccessor(this);
        }
        return bookmarkAccessor;
    }
}
