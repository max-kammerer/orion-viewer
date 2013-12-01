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

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.appcompat.R;
import android.util.DisplayMetrics;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.bookmarks.BookmarkAccessor;

import java.util.Locale;

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

    private OrionViewerActivity viewActivity;

    private LastPageInfo currentBookParameters;

    public void onCreate() {
        instance = this;
        super.onCreate();
        setLanguage(getOptions().getAppLanguage());
    }

    public void setLanguage(String langCode) {
        try {
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = ("DEFAULT".equals(langCode)) ? Locale.getDefault() : new Locale(langCode);
            res.updateConfiguration(conf, dm);
        } catch (Exception e) {
            Common.d("Error setting locale: "  + langCode, e);
        }
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


    public void applyTheme(Activity activity, boolean processActionBar) {
        int themeId = getThemeId(processActionBar);

        if (themeId != -1) {
            activity.setTheme(themeId);
        }
    }

    public void applyTheme(Activity activity) {
        applyTheme(activity, false);
    }

    public boolean isLightTheme() {
        String theme = getOptions().getApplicationTheme();
        if ("DEFAULT".equals(theme) || "DARK".equals(theme)) {
            return false;
        } else if ("LIGHT".equals(theme)) {
            return true;
        }
        return false;
    }

    private int getThemeId(boolean processActionBar) {
        String theme = getOptions().getApplicationTheme();
        boolean showActionBar = !processActionBar || getOptions().isActionBarVisible();
        int themeId = -1;
        if ("DEFAULT".equals(theme) || "DARK".equals(theme)) {
            themeId = showActionBar ? R.style.Theme_AppCompat : universe.constellation.orion.viewer.R.style.MyHolo;
        } else if ("LIGHT".equals(theme)) {
            themeId = showActionBar ? R.style.Theme_AppCompat_Light : universe.constellation.orion.viewer.R.style.MyHoloLight;
        }
        return themeId;
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
		return Build.VERSION.SDK_INT;
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
            Controller controller = viewActivity.getController();
            if (controller != null) {
                if ("walkOrder".equals(key)) {
                    controller.changetWalkOrder((String) value);
                }if ("pageLayout".equals(key)) {
                    controller.changetPageLayout((Integer) value);
                } else if ("contrast".equals(key)) {
                    controller.changeContrast((Integer) value);
                } else if ("threshold".equals(key)) {
                    controller.changeThreshhold((Integer) value);
                } else if ("screenOrientation".equals(key)) {
                    controller.changeOrinatation((String) value);
                }
            }
        }
    }
}
