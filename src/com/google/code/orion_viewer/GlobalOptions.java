package com.google.code.orion_viewer;

import android.os.Environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: mike
 * Date: 26.11.11
 * Time: 16:18
 */
public class GlobalOptions implements Serializable {

    private String lastOpenedDirectory;

    private LinkedList<RecentEntry> recentFiles;

    public GlobalOptions() {
        recentFiles = new LinkedList<RecentEntry>();
        lastOpenedDirectory = null;
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

        if (recentFiles.size() > 10) {
            recentFiles.removeLast();
        }
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
