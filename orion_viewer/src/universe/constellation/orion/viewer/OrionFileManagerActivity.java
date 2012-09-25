/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import pl.polidea.customwidget.TheMissingTabHost;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 16:41
 */
public class OrionFileManagerActivity extends OrionBaseActivity {

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

        updateFileManager();
    }

    public void updateFileManager() {
        ListView view = (ListView) findViewById(R.id.file_chooser);
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

        String startFolder = getStartFolder();

        Common.d("FileManager start folder is " + startFolder);

        view.setAdapter(new FileChooser(this, startFolder, getFileNameFilter()));

        ListView recent = (ListView) findViewById(R.id.recent_list);
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

            recent.setAdapter(new FileChooser(this, globalOptions.getRecentFiles()));
        } else {
            recent.setVisibility(View.GONE);
        }

        updatePathTextView(startFolder);
    }

    private void updatePathTextView(String newPath) {
        TextView path = (TextView) findViewById(R.id.file_manager_path);
        path.setText(newPath);
        device.flushBitmap(100);
    }

    protected void openFile(File file) {
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setClass(getApplicationContext(), OrionViewerActivity.class);
        in.setData(Uri.fromFile(file));
        in.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(in);
    }


    private void initFileManager() {
        TheMissingTabHost host = (TheMissingTabHost) findViewById(R.id.tabhost);
        host.setup();

        TheMissingTabHost.TheMissingTabSpec spec = host.newTabSpec("fileTab");
        spec.setContent(R.id.file_chooser);
        spec.setIndicator("", getResources().getDrawable(R.drawable.folder));
        host.addTab(spec);
        if (showRecentsAndSavePath()) {
            TheMissingTabHost.TheMissingTabSpec recent = host.newTabSpec("recentTab");
            recent.setContent(R.id.recent_list);
            recent.setIndicator("", getResources().getDrawable(R.drawable.book));
            host.addTab(recent);
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

    public GlobalOptions getGlobalOptions() {
        return globalOptions;
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
