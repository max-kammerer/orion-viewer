package com.google.code.orion_viewer;

import android.app.Activity;
import android.content.SharedPreferences;
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

    private static final String RECENT_PREFIX = "recent_";

    private String lastOpenedDirectory;

    private LinkedList<RecentEntry> recentFiles;

    private SharedPreferences prefs;

    public GlobalOptions(Activity activity) {
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

}
