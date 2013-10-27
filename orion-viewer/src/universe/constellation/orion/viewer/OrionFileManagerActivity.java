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

package universe.constellation.orion.viewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 16:41
 */
public class OrionFileManagerActivity extends OrionBaseActivity {

    private static final String LAST_FOLDER = "LAST_FOLDER";

    public static class MyListFragment extends ListFragment {

        protected boolean forFiles = true;

        protected OrionFileManagerActivity activity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (forFiles) {
                activity.createFileView(this);
            } else {
                activity.createRecentView(this);
            }
        }


        @Override
        public void onSaveInstanceState(Bundle outState) {
            if (forFiles) {
                outState.putString(LAST_FOLDER, ((FileChooser)getListAdapter()).getCurrentFolder().getAbsolutePath());
            }
        }
    }

    public class TabListener<T extends MyListFragment> implements ActionBar.TabListener {
        private T mFragment;
        private final OrionBaseActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /** Constructor used each time a new tab is created.
         * @param activity  The host Activity, used to instantiate the fragment
         * @param tag  The identifier tag for the fragment
         * @param clz  The fragment's Class, used to instantiate the fragment
         */
        public TabListener(OrionBaseActivity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        /* The following are each of the ActionBar.TabListener callbacks */

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            //mFragment = (T) mActivity.getSupportFragmentManager().findFragmentByTag(mTag);

            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = (T) Fragment.instantiate(mActivity, mClass.getName());
                mFragment.forFiles = mTag.equals("files");
                mFragment.activity = OrionFileManagerActivity.this;
                ft.replace(android.R.id.tabhost, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
                mFragment = null;
            }
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }

    private SharedPreferences prefs;

    private GlobalOptions globalOptions;

    private boolean justCreated;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(device.getFileManagerLayoutId());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        globalOptions = getOrionContext().getOptions();

        initFileManager();

        justCreated = true;
    }

    protected void onNewIntent(Intent intent) {
        boolean dontStartRecent = intent.getBooleanExtra(DONT_OPEN_RECENT, false);
        if (!dontStartRecent && globalOptions.isOpenRecentBook()) {
            if (globalOptions.getRecentFiles().isEmpty() == false) {
                GlobalOptions.RecentEntry entry = globalOptions.getRecentFiles().get(0);
                File book = new File(entry.getPath());
                if (book.exists()) {
                    openFile(book);
                }
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        justCreated = false;
    }

    protected void onResume() {
        super.onResume();
        if (justCreated) {
            justCreated = false;
            onNewIntent(getIntent());
        }

        updatePathTextView(getStartFolder());
    }

    private void createRecentView(ListFragment list) {
        ListView recent = list.getListView();
        if (showRecentsAndSavePath()) {
            recent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    GlobalOptions.RecentEntry entry = (GlobalOptions.RecentEntry) parent.getItemAtPosition(position);
                    File file = new File(entry.getPath());
                    if (file.exists()) {
                        openFile(file);
                    }
                }
            });

            list.setListAdapter(new FileChooser(this, globalOptions.getRecentFiles()));
        } else {
            recent.setVisibility(View.GONE);
        }
    }

    private void createFileView(ListFragment list) {
        ListView view = list.getListView();
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getItemAtPosition(position);
                if (file.isDirectory()) {
                    File newFolder = ((FileChooser) parent.getAdapter()).changeFolder(file);
                    updatePathTextView(newFolder.getAbsolutePath());
                } else {
                    if (showRecentsAndSavePath()) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Common.LAST_OPENED_DIRECTORY, file.getParentFile().getAbsolutePath());
                        editor.commit();
                    }
                    openFile(file);
                }
            }
        });


        list.setListAdapter(new FileChooser(this, getStartFolder(), getFileNameFilter()));
    }

    private void updatePathTextView(String newPath) {
        getSupportActionBar().setTitle(newPath);
    }

    protected void openFile(File file) {
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setClass(getApplicationContext(), OrionViewerActivity.class);
        in.setData(Uri.fromFile(file));
        in.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(in);
    }


    private void initFileManager() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab = actionBar.newTab()
                .setIcon(R.drawable.folder)
                .setTabListener(new TabListener<MyListFragment>(
                        this, "files", MyListFragment.class));
        actionBar.addTab(tab);

        if (showRecentsAndSavePath()) {
            tab = actionBar.newTab()
                    .setIcon(R.drawable.book)
                    .setTabListener(new TabListener(
                            this, "recent", MyListFragment.class) {
                    });
            actionBar.addTab(tab);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (result) {
            getMenuInflater().inflate(R.menu.file_manager_menu, menu);
        }
        return result;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exit_menu_item:
                finish();
                return true;
        }
        return false;
    }

    //customizable part
    public boolean showRecentsAndSavePath() {
        return true;
    }

    public FilenameFilter getFileNameFilter() {
        return FileChooser.DEFAULT_FILTER;
    }

    public String getStartFolder() {
        String lastOpenedDir = globalOptions.getLastOpenedDirectory();

        if (lastOpenedDir != null && new File(lastOpenedDir).exists()) {
            return lastOpenedDir;
        } else if (new File(Environment.getExternalStorageDirectory() + "/" + device.getDefaultDirectory()).exists()) {
            return Environment.getExternalStorageDirectory() + "/" + device.getDefaultDirectory();
        } else if (new File("/system/media/sdcard/" + device.getDefaultDirectory()).exists()) {
            return "/system/media/sdcard/" + device.getDefaultDirectory();
        } else {
            return Environment.getRootDirectory().getAbsolutePath();
        }
    }
}
