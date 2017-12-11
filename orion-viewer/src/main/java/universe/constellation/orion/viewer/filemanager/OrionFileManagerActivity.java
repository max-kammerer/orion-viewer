/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.filemanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.Permissions;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 16:41
 */
public class OrionFileManagerActivity extends OrionBaseActivity {

    private static final String LAST_FOLDER = "LAST_FOLDER";

    public static class FoldersFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.folder_view, container, false);

        }

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ((OrionFileManagerActivity) getActivity()).
                    createFileView(
                            (ListView) getActivity().findViewById(R.id.listView),
                            (TextView) getActivity().findViewById(R.id.path)
                    );
        }
    }

    public static class RecentListFragment extends ListFragment {
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ((OrionFileManagerActivity) getActivity()).createRecentView(this);
        }
    }

    private SharedPreferences prefs;

    private GlobalOptions globalOptions;

    private boolean justCreated;

    protected void onCreate(Bundle savedInstanceState) {
        super.onOrionCreate(savedInstanceState, R.layout.file_manager);
        log("Creating file manager");

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        globalOptions = getOrionContext().getOptions();

        initFileManager();

        justCreated = true;

        Permissions.checkReadPermission(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Permissions.ORION_ASK_PERMISSION_CODE == requestCode) {
            System.out.println("Permission callback...");
            ListView list = (ListView) findViewById(R.id.listView);
            if (list != null) {
                ListAdapter adapter = list.getAdapter();
                if (adapter instanceof FileChooserAdapter) {
                    File currentFolder = ((FileChooserAdapter) adapter).getCurrentFolder();
                    System.out.println("Refreshing view");
                    ((FileChooserAdapter) adapter).changeFolder(new File(currentFolder.getAbsolutePath()));
                }
            }
        }
    }


    protected void onNewIntent(Intent intent) {
        log("OrionFileManager: On new intent " + intent);

        boolean dontStartRecent = intent.getBooleanExtra(DONT_OPEN_RECENT, false);
        if (!dontStartRecent && globalOptions.isOpenRecentBook()) {
            if (!globalOptions.getRecentFiles().isEmpty()) {
                GlobalOptions.RecentEntry entry = globalOptions.getRecentFiles().get(0);
                File book = new File(entry.getPath());
                if (book.exists()) {
                    log("Opening recent book");
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
    }

    private void createRecentView(ListFragment list) {
        ListView recent = list.getListView();
        recent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GlobalOptions.RecentEntry entry = (GlobalOptions.RecentEntry) parent.getItemAtPosition(position);
                File file = new File(entry.getPath());
                if (file.exists()) {
                    openFile(file);
                }
            }
        });

        list.setListAdapter(new RecentListAdapter(this, globalOptions.getRecentFiles()));
    }

    private void createFileView(ListView list, final TextView path) {
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getItemAtPosition(position);
                if (file.isDirectory()) {
                    File newFolder = ((FileChooserAdapter) parent.getAdapter()).changeFolder(file);
                    path.setText(newFolder.getAbsolutePath());
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

        path.setText(getStartFolder());
        list.setAdapter(new FileChooserAdapter(this, getStartFolder(), getFileNameFilter()));
    }

    protected void openFile(File file) {
        log("Opening new book: " + file.getPath());

        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setClass(getApplicationContext(), OrionViewerActivity.class);
        in.setData(Uri.fromFile(file));
        in.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(in);
    }


    private void initFileManager() {
        SimplePagerAdapter pagerAdapter = new SimplePagerAdapter(getSupportFragmentManager(), showRecentsAndSavePath() ? 2 : 1);
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(pagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        TabLayout.Tab folderTab = tabLayout.getTabAt(0);
        if (folderTab != null) {
            folderTab.setIcon(R.drawable.folder);
        }
        if (showRecentsAndSavePath()) {
            TabLayout.Tab recentTab = tabLayout.getTabAt(1);
            if (recentTab != null) {
                recentTab.setIcon(R.drawable.book);
            }
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
        return FileChooserAdapter.DEFAULT_FILTER;
    }

    public String getStartFolder() {
        String lastOpenedDir = globalOptions.getLastOpenedDirectory();

        if (lastOpenedDir != null && new File(lastOpenedDir).exists()) {
            return lastOpenedDir;
        }

        String path = Environment.getExternalStorageDirectory().getPath() + "/";
        if (new File(path).exists()) {
            return path;
        }

        String path1 = "/system/media/sdcard/";
        if (new File(path1).exists()) {
            return path1;
        }

        return Environment.getRootDirectory().getAbsolutePath();
    }
}


class SimplePagerAdapter extends FragmentStatePagerAdapter {
    private final int pageCount;

    public SimplePagerAdapter(FragmentManager fm, int pageCount) {
        super(fm);
        this.pageCount = pageCount;
    }

    @Override
    public Fragment getItem(int i) {
        return i == 0 ?
                new OrionFileManagerActivity.FoldersFragment() : new OrionFileManagerActivity.RecentListFragment();
    }

    @Override
    public int getCount() {
        return pageCount;
    }

}
