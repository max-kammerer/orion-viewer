package com.google.code.orion_viewer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.os.Environment;
import android.preference.PreferenceManager;

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

    private String lastOpenedDirectory;

    private LinkedList<RecentEntry> recentFiles;

    private SharedPreferences prefs;

    private int nextKey;

    private int prevKey;

    private boolean swapKeys;

    private boolean useNookKeys;

    private int defaultOrientation;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

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
        if (device != null) {
            onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences preferences, String s) {
                    Common.d("onSharedPreferenceChanged " + s);
                    if (NEXT_KEY.equals(s)) {
                        nextKey = preferences.getInt(NEXT_KEY, 0);
                    } else if (PREV_KEY.equals(s)) {
                        prevKey = preferences.getInt(PREV_KEY, 0);
                    } else if (SWAP_KEYS.equals(s)) {
                        swapKeys = preferences.getBoolean(SWAP_KEYS, false);
                    } else if (USE_NOOK_KEYS.equals(s)) {
                        useNookKeys = preferences.getBoolean(USE_NOOK_KEYS, false);
                    } else if (DEFAULT_ORIENTATION.equals(s)) {
                        defaultOrientation = Integer.parseInt(preferences.getString(DEFAULT_ORIENTATION, "0"));
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

    public void setLastOpenedDirectory(String lastOpenedDirectory) {
        this.lastOpenedDirectory = lastOpenedDirectory;
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

    public String getDefaultDirectory() {
        return Environment.getRootDirectory().getAbsolutePath();
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
}
