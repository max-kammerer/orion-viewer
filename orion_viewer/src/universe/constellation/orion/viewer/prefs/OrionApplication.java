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

import android.app.Activity;
import android.app.Application;
import android.preference.PreferenceManager;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.db.BookmarkAccessor;

import java.lang.reflect.Field;

/**
 * User: mike
 * Date: 23.01.12
 * Time: 20:03
 */
public class OrionApplication extends Application {

    private GlobalOptions options;

    private GlobalOptions keyBinding;

    private TemporaryOptions tempOptions;

    public static OrionApplication instance;

    private BookmarkAccessor bookmarkAccessor;

    private int sdk_version = -1;

    private OrionViewerActivity viewActivity;

    private LastPageInfo currentBookParameters;

    public void onCreate() {
        instance = this;
        super.onCreate();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public GlobalOptions getOptions() {
        if (options == null) {
            options = new GlobalOptions(this, PreferenceManager.getDefaultSharedPreferences(this), true);
        }
        return options;
    }

    public GlobalOptions getKeyBinding() {
        if (keyBinding == null) {
            keyBinding = new GlobalOptions(this, getSharedPreferences("key_binding", MODE_PRIVATE), false);
        }
        return keyBinding;
    }

    public void onNewBook(String fileName) {
        tempOptions = new TemporaryOptions();
        tempOptions.openedFile = fileName;
    }

    public TemporaryOptions getTempOptions() {
        return tempOptions;
    }


    public void applyTheme(Activity activity) {
        String theme = getOptions().getApplicationTheme();
        if ("DEFAULT".equals(theme)) {
            return;
        }

        boolean haveTitleBar = false;

        int themeId = -1;
        if ("DARK".equals(theme)) {
            themeId =  haveTitleBar ? android.R.style.Theme_Black : android.R.style.Theme_Black_NoTitleBar;
        } else if ("LIGHT".equals(theme)) {
            themeId = haveTitleBar ? android.R.style.Theme_Light : android.R.style.Theme_Light_NoTitleBar;
        }

        if (themeId != -1) {
            activity.setTheme(themeId);
        }
    }

    public BookmarkAccessor getBookmarkAccessor() {
        if (bookmarkAccessor == null) {
            bookmarkAccessor = new BookmarkAccessor(this);
        }
        return bookmarkAccessor;
    }

    public void destroyDb() {
        if (bookmarkAccessor != null) {
            bookmarkAccessor.close();
            bookmarkAccessor = null;
        }
    }

    public int getSdkVersion() {
		if (sdk_version < 0) {
            sdk_version = 3;
            Field fld;
            try {
                Class<?> cl = android.os.Build.VERSION.class;
                fld = cl.getField("SDK_INT");
                sdk_version = fld.getInt(cl);
            } catch (Exception e) {
                 //Android 1.5
            }
        }
		return sdk_version;
	}

    public LastPageInfo getCurrentBookParameters() {
        return currentBookParameters;
    }

    public void setCurrentBookParameters(LastPageInfo currentBookParameters) {
        this.currentBookParameters = currentBookParameters;
    }


    public OrionViewerActivity getViewActivity() {
        return viewActivity;
    }

    public void setViewActivity(OrionViewerActivity viewActivity) {
        this.viewActivity = viewActivity;
    }

    //temporary hack
    public void processBookOptionChange(String key, Object value) {
        if (viewActivity != null) {
            if ("contrast".equals(key)) {
                Controller controller = viewActivity.getController();
                if (controller != null) {
                    controller.changeContrast((Integer)value);
                }
            }
        }
    }
}
