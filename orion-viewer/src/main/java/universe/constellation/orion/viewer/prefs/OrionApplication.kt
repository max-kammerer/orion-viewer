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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import java.util.Locale;

import universe.constellation.orion.viewer.BuildConfig;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.device.Device;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.bookmarks.BookmarkAccessor;
import universe.constellation.orion.viewer.device.EInkDeviceWithoutFastRefresh;

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

    private Device device = Common.createDevice();

    public boolean isTesting = false;

    private String langCode;

    public void onCreate() {
        instance = this;
        super.onCreate();
        GlobalOptions options = getOptions();
        setLangCode(options.getAppLanguage());
        Common.logOrionAndDeviceInfo();
        if (device instanceof EInkDeviceWithoutFastRefresh) {
            String version = options.getVersion();
            if (options.isShowTapHelp() || VersionUtilKt.isVersionEquals("0.0.0", version)) {
                try {
                    SharedPreferences prefs = options.prefs;
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(GlobalOptions.DRAW_OFF_PAGE, false);
                    edit.putString(GlobalOptions.VERSION, BuildConfig.VERSION_NAME);
                    edit.commit();
                } catch (Exception e) {
                    Common.d(e);
                }
            }
        }
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
        updateLanguage(getResources());
    }

    public void updateLanguage(Resources res) {
        try {
            Locale defaultLocale = Locale.getDefault();
            Common.d("Updating locale to " + langCode + " from " + defaultLocale.getLanguage());
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = (langCode == null || "DEFAULT".equals(langCode)) ? defaultLocale : new Locale(langCode);
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


    public void applyTheme(Activity activity) {
        int themeId = getThemeId();

        if (themeId != -1) {
            activity.setTheme(themeId);
        }
    }

    public boolean isLightTheme() {
        String theme = getOptions().getApplicationTheme();
        boolean isDefault = !("DARK".equals(theme) || "LIGHT".equals(theme));
        boolean useDarkTheme = isDefault ? device.isDefaultDarkTheme() : false;

        if (useDarkTheme || "DARK".equals(theme)) {
            return false;
        }

        return true;
    }

    private int getThemeId() {
        return !isLightTheme() ?
                R.style.Theme_AppCompat_NoActionBar :
                R.style.Theme_AppCompat_Light_NoActionBar;
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
                } else if ("pageLayout".equals(key)) {
                    controller.changetPageLayout((Integer) value);
                } else if ("contrast".equals(key)) {
                    controller.changeContrast((Integer) value);
                } else if ("threshold".equals(key)) {
                    controller.changeThreshhold((Integer) value);
                } else if ("screenOrientation".equals(key)) {
                    controller.changeOrinatation((String) value);
                } else if ("colorMode".equals(key)) {
                    controller.changeColorMode((String) value, true);
                } else if ("zoom".equals(key)) {
                    controller.changeZoom((Integer) value);
                }
            }
        }
    }

    public Device getDevice() {
        return device;
    }
}
