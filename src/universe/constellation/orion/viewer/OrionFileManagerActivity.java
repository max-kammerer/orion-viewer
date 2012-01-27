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
import com.google.code.orion_viewer.*;
import pl.polidea.customwidget.TheMissingTabHost;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 16:41
 */
public class OrionFileManagerActivity extends OrionBaseActivity {

    private SharedPreferences prefs;

    private GlobalOptions globalOptions;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(device.getFileManagerLayoutId());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        globalOptions = getOrionContext().getOptions();
        initFileManager();
    }

    protected void onResume() {
        super.onResume();
        //globalOptions = new GlobalOptions(this);
        updateFileManager();
    }

    public void updateFileManager() {
        ListView view = (ListView) findViewById(R.id.file_chooser);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) parent.getItemAtPosition(position);
                if (file.isDirectory()) {
                    File newFolder = ((FileChooser) parent.getAdapter()).changeFolder(file);
                    updatePath(newFolder.getAbsolutePath());
                } else {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(Common.LAST_OPENED_DIRECTORY, file.getParentFile().getAbsolutePath());
                    editor.commit();
                    openFile(file);
                }
            }
        });

        String startFolder = null;
        String lastOpenedDir = globalOptions.getLastOpenedDirectory();

        if (lastOpenedDir != null && new File(lastOpenedDir).exists()) {
            startFolder = lastOpenedDir;
        } else if (new File(Environment.getExternalStorageDirectory() + "/" + device.getDefaultDirectory()).exists()) {
            startFolder = Environment.getExternalStorageDirectory() + "/" + device.getDefaultDirectory();
        } else if (new File("/system/media/sdcard/" + device.getDefaultDirectory()).exists()) {
            startFolder = "/system/media/sdcard/" + device.getDefaultDirectory();
        } else {
            startFolder = Environment.getRootDirectory().getAbsolutePath();
        }

        Common.d("FileManager start folder is " + startFolder);

        view.setAdapter(new FileChooser(this, startFolder));

        ListView recent = (ListView) findViewById(R.id.recent_list);
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

        updatePath(startFolder);
    }

    private void updatePath(String newPath) {
        TextView path = (TextView) findViewById(R.id.file_manager_path);
        path.setText(newPath);
        device.flushBitmap(100);
    }

    private void openFile(File file) {
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setClass(getApplicationContext(), OrionViewerActivity.class);
        in.setData(Uri.fromFile(file));
        in.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(in);
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if (((TheMissingTabHost) findViewById(R.id.tabhost)).getCurrentTab() == 0) {
//                ((FileChooser) ((ListView) findViewById(R.id.file_chooser)).getAdapter()).goToParent();
//            }
//            return true;
//        }
//        return false;
//    }

    private void initFileManager() {
        TheMissingTabHost host = (TheMissingTabHost) findViewById(R.id.tabhost);
        host.setup();

        TheMissingTabHost.TheMissingTabSpec spec = host.newTabSpec("fileTab");
        spec.setContent(R.id.file_chooser);
        spec.setIndicator("", getResources().getDrawable(R.drawable.folder));
        host.addTab(spec);
        TheMissingTabHost.TheMissingTabSpec recent = host.newTabSpec("recentTab");
        recent.setContent(R.id.recent_list);
        recent.setIndicator("", getResources().getDrawable(R.drawable.book));
        host.addTab(recent);
        //host.setCurrentTab(0);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
