package com.google.code.orion_viewer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import universe.constellation.orion.viewer.prefs.OrionTapActivity;

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

    //public final static String USE_NOOK_KEYS = "USE_NOOK_KEYS";

    public final static String DEFAULT_ORIENTATION = "BOOK_ORIENTATION";

    public final static String APPLY_AND_CLOSE = "APPLY_AND_CLOSE";

    public final static String FULL_SCREEN = "FULL_SCREEN";

    public final static String TAP_ZONE = "TAP_ZONE";

    public final static String SCREEN_ORIENTATION = "SCREEN_ORIENTATION";

    public final static String EINK_OPTIMIZATION = "EINK_OPTIMIZATION";

    public final static String EINK_TOTAL_AFTER = "EINK_TOTAL_AFTER";

    public final static String DICTIONARY = "DICTIONARY";

    private String lastOpenedDirectory;

    private LinkedList<RecentEntry> recentFiles;

    private SharedPreferences prefs;

    private int nextKey = -1;

    private int prevKey = -1;

    private boolean swapKeys;

    //private boolean useNookKeys;

    private int defaultOrientation;

    private boolean applyAndClose;

    private boolean fullScreen;

    private String dictionary;

    private boolean einkOptimization;

    private int einkRefresafter;

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

        nextKey = prefs.getInt(NEXT_KEY, -1);
        prevKey = prefs.getInt(PREV_KEY, -1);

        swapKeys = prefs.getBoolean(SWAP_KEYS, false);
        //useNookKeys = prefs.getBoolean(USE_NOOK_KEYS, false);
        defaultOrientation = Integer.parseInt(prefs.getString(DEFAULT_ORIENTATION, "0"));
        applyAndClose = prefs.getBoolean(APPLY_AND_CLOSE, false);
        fullScreen = prefs.getBoolean(FULL_SCREEN, false);
        dictionary = prefs.getString(DICTIONARY, "FORA");
        einkOptimization = prefs.getBoolean(EINK_OPTIMIZATION, false);
        einkRefresafter = getInt(EINK_TOTAL_AFTER, 10);
        resetTapCodes();

        if (device != null) {
            onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences preferences, String name) {
                    Common.d("onSharedPreferenceChanged " + name);
                    if (NEXT_KEY.equals(name)) {
                        nextKey = preferences.getInt(NEXT_KEY, -1);
                    } else if (PREV_KEY.equals(name)) {
                        prevKey = preferences.getInt(PREV_KEY, -1);
                    } else if (SWAP_KEYS.equals(name)) {
                        swapKeys = preferences.getBoolean(SWAP_KEYS, false);
//                    } else if (USE_NOOK_KEYS.equals(name)) {
//                        useNookKeys = preferences.getBoolean(USE_NOOK_KEYS, false);
                    } else if (DEFAULT_ORIENTATION.equals(name)) {
                        defaultOrientation = Integer.parseInt(preferences.getString(DEFAULT_ORIENTATION, "0"));
                    } else if (APPLY_AND_CLOSE.equals(name)) {
                        applyAndClose = preferences.getBoolean(APPLY_AND_CLOSE, false);
                    } else if (FULL_SCREEN.equals(name)) {
                        fullScreen = preferences.getBoolean(FULL_SCREEN, false);
                    } else if (name.startsWith(TAP_ZONE)) {
                        resetTapCodes();
                    } else if (DICTIONARY.equals(name)) {
                        dictionary = preferences.getString(DICTIONARY, "FORA");
                    } else if (EINK_OPTIMIZATION.equals(name)) {
                        einkOptimization = prefs.getBoolean(EINK_OPTIMIZATION, false);
                    } else if (EINK_TOTAL_AFTER.equals(name)) {
                        einkRefresafter = getInt(EINK_TOTAL_AFTER, 10);
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

//    public boolean isUseNookKeys() {
//        return useNookKeys;
//    }

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

    public String getDictionary() {
        return dictionary;
    }

    public int getEinkRefresafter() {
        return einkRefresafter;
    }

    public boolean isEinkOptimization() {
        return einkOptimization;
    }

    private int getInt(String name, int defaultValue) {
        String value = prefs.getString(EINK_TOTAL_AFTER, null);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
