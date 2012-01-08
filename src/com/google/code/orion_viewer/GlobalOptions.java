package com.google.code.orion_viewer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.google.code.orion_viewer.prefs.OrionTapActivity;

import java.io.Serializable;
import java.util.*;

/**
 * User: mike
 * Date: 26.11.11
 * Time: 16:18
 */
public class GlobalOptions implements Serializable {

    public static final int MAX_RECENT_ENTRIES = 10;

    public static final String NEXT_KEY = "next_key_keycode";

    public static final String PREV_KEY = "prev_key_keycode";

    private static final String RECENT_PREFIX = "recent_";

    public final static String SWAP_KEYS = "SWAP_KEYS";

    public final static String USE_NOOK_KEYS = "USE_NOOK_KEYS";

    public final static String DEFAULT_ORIENTATION = "DEFAULT_ORIENTATION";

    public final static String APPLY_AND_CLOSE = "APPLY_AND_CLOSE";

    public final static String FULL_SCREEN = "FULL_SCREEN";

    public final static String TAP_ZONE = "TAP_ZONE";

    private String lastOpenedDirectory;

    private LinkedList<RecentEntry> recentFiles;

    private SharedPreferences prefs;

    private int nextKey;

    private int prevKey;

    private boolean swapKeys;

    private boolean useNookKeys;

    private int defaultOrientation;

    private boolean applyAndClose;

    private boolean fullScreen;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    private int [][] tapCodes = new int [9][2];

    public GlobalOptions(Activity activity) {
        this(activity, null);
    }

    public GlobalOptions(Activity activity, final Device device) {
        prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        lastOpenedDirectory = prefs.getString(Common.LAST_OPENED_DIRECTORY, null);

        recentFiles = new LinkedList<RecentEntry>();
        for (int i = 0; i < MAX_RECENT_ENTRIES; i++) {
            String entry = prefs.getString(RECENT_PREFIX + i, null);
            if (entry == null) {
                break;
            } else {
                recentFiles.add(new RecentEntry(entry));
            }
        }

        nextKey = prefs.getInt(NEXT_KEY, 0);
        prevKey = prefs.getInt(PREV_KEY, 0);

        swapKeys = prefs.getBoolean(SWAP_KEYS, false);
        useNookKeys = prefs.getBoolean(USE_NOOK_KEYS, false);
        defaultOrientation = Integer.parseInt(prefs.getString(DEFAULT_ORIENTATION, "0"));
        applyAndClose = prefs.getBoolean(APPLY_AND_CLOSE, false);
        fullScreen = prefs.getBoolean(FULL_SCREEN, false);
        resetTapCodes();

        if (device != null) {
            onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences preferences, String name) {
                    Common.d("onSharedPreferenceChanged " + name);
                    if (NEXT_KEY.equals(name)) {
                        nextKey = preferences.getInt(NEXT_KEY, 0);
                    } else if (PREV_KEY.equals(name)) {
                        prevKey = preferences.getInt(PREV_KEY, 0);
                    } else if (SWAP_KEYS.equals(name)) {
                        swapKeys = preferences.getBoolean(SWAP_KEYS, false);
                    } else if (USE_NOOK_KEYS.equals(name)) {
                        useNookKeys = preferences.getBoolean(USE_NOOK_KEYS, false);
                    } else if (DEFAULT_ORIENTATION.equals(name)) {
                        defaultOrientation = Integer.parseInt(preferences.getString(DEFAULT_ORIENTATION, "0"));
                    } else if (APPLY_AND_CLOSE.equals(name)) {
                        applyAndClose = preferences.getBoolean(APPLY_AND_CLOSE, false);
                    } else if (FULL_SCREEN.equals(FULL_SCREEN)) {
                        fullScreen = preferences.getBoolean(FULL_SCREEN, false);
                    } else if (name.startsWith(TAP_ZONE)) {
                        resetTapCodes();
                    }
                    device.updateOptions(GlobalOptions.this);
                }
            };

            PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    public String getLastOpenedDirectory() {
        return lastOpenedDirectory;
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

    public void saveDirectory() {
        //TODO
    }

    public void saveRecents() {
        int i = 0;
        SharedPreferences.Editor editor = prefs.edit();
        for (Iterator<RecentEntry> iterator = recentFiles.iterator(); iterator.hasNext(); i++) {
            RecentEntry next =  iterator.next();
            editor.putString(RECENT_PREFIX  + i, next.getPath());
        }
        editor.commit();
    }

    public static class RecentEntry implements Serializable{

        private String path;

        public RecentEntry(String path) {
            this.path = path;
        }


        public String getPath() {
            return path;
        }

        public String getLastPathElement() {
            return path.substring(path.lastIndexOf("/") + 1);
        }

        @Override
        public String toString() {
            return getLastPathElement();
        }
    }

    public LinkedList<RecentEntry> getRecentFiles() {
        return recentFiles;
    }

    public int getPrevKey() {
        return prevKey;
    }

    public int getNextKey() {
        return nextKey;
    }

    public void onDestroy(Activity activity) {
        if (onSharedPreferenceChangeListener != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

    public boolean isSwapKeys() {
        return swapKeys;
    }

    public boolean isUseNookKeys() {
        return useNookKeys;
    }

    public int getDefaultOrientation() {
        return defaultOrientation;
    }

    public boolean isApplyAndClose() {
        return applyAndClose;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public int getActionCode(int i, int j, boolean isLong) {
        int code = tapCodes[i * 3 + j][isLong ? 1 : 0];
        if (code == -1) {
            code = prefs.getInt(OrionTapActivity.getKey(i, j, isLong), -1);
            if (code == -1) {
                code = OrionTapActivity.getDefaultAction(i, j, isLong);
            }
            tapCodes[i * 3 + j][isLong ? 1 : 0] = code;
        }
        return code;
    }


    private void resetTapCodes() {
        for (int i = 0; i < tapCodes.length; i++) {
            int[] tapCode = tapCodes[i];
            for (int j = 0; j < tapCode.length; j++) {
                tapCode[j] = -1;
            }
        }
    }


}
