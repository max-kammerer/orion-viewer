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

import static universe.constellation.orion.viewer.LoggerKt.log;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import universe.constellation.orion.viewer.OptionActions;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.PageOptions;
import universe.constellation.orion.viewer.PageWalker;
import universe.constellation.orion.viewer.device.EInkDeviceWithoutFastRefresh;
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity;

public class GlobalOptions implements Serializable, PageOptions {

    public static final int MAX_RECENT_ENTRIES = 20;

    public static final String NEXT_KEY = "next_key_keycode";

    public static final String PREV_KEY = "prev_key_keycode";

    private static final String RECENT_PREFIX = "recent_";

    public final static String SWAP_KEYS = "SWAP_KEYS";

    public final static String DEFAULT_ORIENTATION = "BOOK_ORIENTATION";

    public final static String DEFAULT_ZOOM = "DEFAULT_ZOOM";

    public final static String DEFAULT_CONTRAST = "DEFAULT_CONTRAST_3";

    public final static String APPLY_AND_CLOSE = "APPLY_AND_CLOSE";

    public final static String FULL_SCREEN = "FULL_SCREEN";

    public final static String DRAW_OFF_PAGE = "DRAW_OFF_PAGE";

    public final static String SHOW_ACTION_BAR = "SHOW_ACTION_BAR";

    public final static String SHOW_STATUS_BAR = "SHOW_STATUS_BAR";

    public final static String NEW_UI = "NEW_UI";

    public final static String SHOW_OFFSET_ON_STATUS_BAR = "SHOW_OFFSET_ON_STATUS_BAR";

    public final static String TAP_ZONE = "TAP_ZONE";

    public final static String SCREEN_ORIENTATION = "SCREEN_ORIENTATION";

    public final static String EINK_OPTIMIZATION = "EINK_OPTIMIZATION";

    public final static String EINK_TOTAL_AFTER = "EINK_TOTAL_AFTER";

    public final static String DICTIONARY = "DICTIONARY";

    public final static String LONG_CROP_VALUE = "LONG_CROP_VALUE";

    public final static String SCREEN_OVERLAPPING_HORIZONTAL = "SCREEN_OVERLAPPING_HORIZONTAL";

    public final static String SCREEN_OVERLAPPING_VERTICAL = "SCREEN_OVERLAPPING_VERTICAL";

    public final static String DEBUG = "DEBUG";

    public final static String BRIGHTNESS = "BRIGHTNESS";

    public final static String CUSTOM_BRIGHTNESS = "CUSTOM_BRIGHTNESS";

    public final static String APPLICATION_THEME = "APPLICATION_THEME";

    public final static String APPLICATION_THEME_DEFAULT = "DEFAULT";

    public final static String APP_LANGUAGE = "LANGUAGE";

    public final static String OPEN_RECENT_BOOK = "OPEN_RECENT_BOOK";

    public final static String DAY_NIGHT_MODE = "DAY_NIGHT_MODE";

    public final static String WALK_ORDER = "WALK_ORDER";

    public final static String PAGE_LAYOUT = "PAGE_LAYOUT";

    public final static String COLOR_MODE = "COLOR_MODE";

    public final static String SHOW_TAP_HELP = "SHOW_TAP_HELP";

    public final static String TEST_SCREEN_WIDTH = "TEST_SCREEN_WIDTH";

    public final static String TEST_SCREEN_HEIGHT = "TEST_SCREEN_HEIGHT";

    public final static String OPEN_AS_TEMP_BOOK = "OPEN_AS_TEMP_BOOK";

    public final static String SCREEN_BACKLIGHT_TIMEOUT = "SCREEN_BACKLIGHT_TIMEOUT";

    public final static String ENABLE_TOUCH_MOVE = "ENABLE_TOUCH_MOVE";

    public final static String ENABLE_MOVE_ON_PINCH_ZOOM = "ENABLE_MOVE_ON_PINCH_ZOOM";

    public final static String VERSION = "VERSION";

    public final static String DEFAULT_LANGUAGE = "DEFAULT";

    private LinkedList<RecentEntry> recentFiles;

    protected final SharedPreferences prefs;


    /* Caution: The preference manager does not currently store a strong reference to the listener.
    You must store a strong reference to the listener, or it will be susceptible to garbage collection.
    We recommend you keep a reference to the listener in the instance data of an object that will exist as long as you need the listener. */
    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    GlobalOptions(final OrionApplication context, SharedPreferences preferences, boolean loadRecents) {
        prefs = preferences;

        if (loadRecents) {
            recentFiles = new LinkedList<>();
            for (int i = 0; i < MAX_RECENT_ENTRIES; i++) {
                String entry = prefs.getString(RECENT_PREFIX + i, null);
                if (entry == null) {
                    break;
                } else {
                    recentFiles.add(new RecentEntry(entry));
                }
            }
        }

        //TODO ?
        onSharedPreferenceChangeListener = (preferences1, name) -> {
            log("onSharedPreferenceChanged " + name);

            OrionViewerActivity activity = context.getViewActivity();
            if (activity != null) {
                if (FULL_SCREEN.equals(name)) {
                    OptionActions.FULL_SCREEN.doAction(activity, false, isFullScreen());
                } else if (SHOW_ACTION_BAR.equals(name)) {
                    OptionActions.SHOW_ACTION_BAR.doAction(activity, false, isActionBarVisible());
                } else if (SHOW_STATUS_BAR.equals(name)) {
                    OptionActions.SHOW_STATUS_BAR.doAction(activity, false, isStatusBarVisible());
                } else if (SHOW_OFFSET_ON_STATUS_BAR.equals(name)) {
                    OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(activity, false, isShowOffsetOnStatusBar());
                } else if (SCREEN_OVERLAPPING_HORIZONTAL.equals(name)) {
                    OptionActions.SCREEN_OVERLAPPING_HORIZONTAL.doAction(activity, getHorizontalOverlapping(), getVerticalOverlapping());
                } else if (SCREEN_OVERLAPPING_VERTICAL.equals(name)) {
                    OptionActions.SCREEN_OVERLAPPING_VERTICAL.doAction(activity, getHorizontalOverlapping(), getVerticalOverlapping());
                } else if (APP_LANGUAGE.equals(name)) {
                    context.setLanguage(getAppLanguage());
                } else if (DRAW_OFF_PAGE.equals(name)) {
                    activity.getFullScene().setDrawOffPage(isDrawOffPage());
                    //TODO ?
                    activity.getView().invalidate();
                }
            }

            if (DEBUG.equals(name)) {
                context.startOrStopDebugLogger(getBooleanProperty(DEBUG, false));
            }

        };

        prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }



    public String getLastOpenedDirectory() {
        return getStringProperty(OrionFileManagerActivity.LAST_OPENED_DIRECTORY, null);
    }

    public void addRecentEntry(RecentEntry newEntry) {
        for (Iterator<RecentEntry> iterator = recentFiles.iterator(); iterator.hasNext(); ) {
            RecentEntry recentEntry =  iterator.next();
            if (recentEntry.getPath().equals(newEntry.getPath())) {
                iterator.remove();
                break;
            }
        }

        recentFiles.add(0, newEntry);

        if (recentFiles.size() > MAX_RECENT_ENTRIES) {
            recentFiles.removeLast();
        }
    }

    public void saveProperty(String property, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(property, value);
        editor.apply();
    }

    public void saveRecents() {
        int i = 0;
        SharedPreferences.Editor editor = prefs.edit();
        for (Iterator<RecentEntry> iterator = recentFiles.iterator(); iterator.hasNext(); i++) {
            RecentEntry next =  iterator.next();
            editor.putString(RECENT_PREFIX  + i, next.getPath());
        }
        editor.apply();
    }

    public static class RecentEntry implements Serializable{

        private final String path;

        public RecentEntry(String path) {
            this.path = path;
        }


        public String getPath() {
            return path;
        }

        public String getLastPathElement() {
            return path.substring(path.lastIndexOf("/") + 1);
        }

        @NonNull
        @Override
        public String toString() {
            return getLastPathElement();
        }
    }

    public LinkedList<RecentEntry> getRecentFiles() {
        return recentFiles;
    }

//    public void onDestroy(Context applicationContext) {
//        if (onSharedPreferenceChangeListener != null) {
//            PreferenceManager.getDefaultSharedPreferences(applicationContext).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
//            onSharedPreferenceChangeListener = null;
//        }
//    }

    public boolean isSwapKeys() {
        return getBooleanProperty(SWAP_KEYS, false);
    }

    public boolean isEnableTouchMove() {
        return getBooleanProperty(ENABLE_TOUCH_MOVE, true);
    }

    public boolean isEnableMoveOnPinchZoom() {
        return getBooleanProperty(ENABLE_MOVE_ON_PINCH_ZOOM, false);
    }

    public int getDefaultOrientation() {
        return getIntFromStringProperty(DEFAULT_ORIENTATION, 0);
    }

    public int getDefaultZoom() {
        return getIntFromStringProperty(DEFAULT_ZOOM, 0);
    }

    public int getDefaultContrast() {
        return getIntFromStringProperty(DEFAULT_CONTRAST, 100);
    }

    public boolean isApplyAndClose() {
        return getBooleanProperty(APPLY_AND_CLOSE, false);
    }

    public boolean isFullScreen() {
        return getBooleanProperty(FULL_SCREEN, false);
    }

    public boolean isDrawOffPage() {
        return getBooleanProperty(DRAW_OFF_PAGE, !(OrionApplication.Companion.getInstance().getDevice() instanceof EInkDeviceWithoutFastRefresh));
    }

    public boolean isActionBarVisible() {
        return getBooleanProperty(SHOW_ACTION_BAR, true);
    }

    public boolean isShowTapHelp() {
        return getBooleanProperty(SHOW_TAP_HELP, true);
    }

    public boolean isStatusBarVisible() {
        return getBooleanProperty(SHOW_STATUS_BAR, true);
    }

    public boolean isNewUI() {
        return getBooleanProperty(NEW_UI, false);
    }

    public boolean isShowOffsetOnStatusBar() {
        return getBooleanProperty(SHOW_OFFSET_ON_STATUS_BAR, true);
    }

    public int getActionCode(int i, int j, boolean isLong) {
        String key = OrionTapActivity.getKey(i, j, isLong);
        int code = getInt(key, -1);
        if (code == -1) {
            code = getInt(key, OrionTapActivity.getDefaultAction(i, j, isLong));
        }
        return code;
    }

    public String getDictionary() {
        return getStringProperty(DICTIONARY, "FORA");
    }

    public int getEinkRefreshAfter() {
        return getIntFromStringProperty(EINK_TOTAL_AFTER, 10);
    }

    public boolean isEinkOptimization() {
        return getBooleanProperty(EINK_OPTIMIZATION, false);
    }

    public int getIntFromStringProperty(String key, int defaultValue) {
        String value = prefs.getString(key, null);
        int newIntValue;
        if (value == null || "".equals(value)) {
            newIntValue = defaultValue;
        } else {
            newIntValue = Integer.parseInt(value);
        }
        return newIntValue;
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public String getStringProperty(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }


    public void saveBooleanProperty(String key, boolean newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, newValue);
        editor.apply();
    }


    public int getLongCrop() {
        return getIntFromStringProperty(LONG_CROP_VALUE, 10);
    }

    public int getVerticalOverlapping() {
        return getIntFromStringProperty(SCREEN_OVERLAPPING_VERTICAL, 3);
    }

    public int getHorizontalOverlapping() {
        return getIntFromStringProperty(SCREEN_OVERLAPPING_HORIZONTAL, 3);
    }

    public int getBrightness() {
        return getIntFromStringProperty(BRIGHTNESS, 100);
    }

    public boolean isCustomBrightness() {
        return getBooleanProperty(CUSTOM_BRIGHTNESS, false);
    }

    public boolean isOpenRecentBook() {
        return getBooleanProperty(OPEN_RECENT_BOOK, false);
    }


    public String getApplicationTheme() {
        return getStringProperty(APPLICATION_THEME, APPLICATION_THEME_DEFAULT);
    }

    public String getAppLanguage() {
        return getStringProperty(APP_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public String getWalkOrder() {
        return getStringProperty(WALK_ORDER, PageWalker.WALK_ORDER.ABCD.name());
    }

    public int getPageLayout() {
        return getInt(PAGE_LAYOUT, 0);
    }

    public String getColorMode() {
        return getStringProperty(COLOR_MODE, "CM_NORMAL");
    }

    public int getScreenBacklightTimeout(int defaultValue) {
        return getIntFromStringProperty(SCREEN_BACKLIGHT_TIMEOUT, defaultValue);
    }


//    public void subscribe(PrefListener listener) {
//        prefListener.add(listener);
//    }
//
//    private void pushChangePropertyEvent(String key, Object oldValue) {
//        for (int i = 0; i < prefListener.size(); i++) {
//            PrefListener prefListener1 =  prefListener.get(i);
//            try {
//                prefListener1.onPreferenceChanged(this, key, oldValue);
//            } catch (Exception e) {
//                log(e);
//                //TODO show error
//            }
//        }
//    }
//
//
//    public void unsubscribe(PrefListener listener) {
//        prefListener.remove(listener);
//    }

    public void removePreference(String name) {
        prefs.edit().remove(name).apply();
    }

    public void putIntPreference(String name, int value) {
        prefs.edit().putInt(name, value).apply();
    }

    public void removeAll() {
        prefs.edit().clear().apply();
    }

    public Map<String, ?> getAllProperties() {
        return prefs.getAll();
    }

}
