package universe.constellation.orion.viewer;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.Device;
import com.google.code.orion_viewer.FileChooser;
import com.google.code.orion_viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.db.Bookmark;
import universe.constellation.orion.viewer.db.BookmarkAccessor;
import universe.constellation.orion.viewer.db.BookmarkExporter;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

/**
 * User: mike
 * Date: 25.02.12
 * Time: 16:20
 */
public class OrionBookmarkActivity extends OrionBaseActivity {

    public static final String OPEN_PAGE = "open_page";

    public static final String OPENED_FILE = "opened_file";

    public static final String BOOK_ID = "book_id";

    private long bookId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Device.Info.NOOK_CLASSIC ? R.layout.nook_bookmarks : R.layout.bookmarks);

        onNewIntent(getIntent());

        ListView view = (ListView) findMyViewById(R.id.bookmarks);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bookmark bookmark = (Bookmark) parent.getItemAtPosition(position);
                Intent result = new Intent();
                result.putExtra(OPEN_PAGE, bookmark.page);
                System.out.println("bookmark id = " + bookmark.id + " page = " + bookmark.page);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        ImageButton menu = (ImageButton) findMyViewById(R.id.nook_bookmarks_menu);
        if (menu != null) {
            menu.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v) {
                    openOptionsMenu();
                }
            });
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        BookmarkAccessor accessor = getOrionContext().getBookmarkAccessor();
        bookId = intent.getLongExtra(BOOK_ID, -1);
        List bookmarks = accessor.selectBookmarks(bookId);
        ListView view = (ListView) findMyViewById(R.id.bookmarks);
        view.setAdapter(new ArrayAdapter(this, R.layout.bookmark_entry, R.id.bookmark_entry, bookmarks) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = super.getView(position, convertView, parent);
                Bookmark item = (Bookmark) getItem(position);
                TextView page = (TextView) convertView.findViewById(R.id.bookmark_entry_page);
                page.setText("" + (item.page == - 1 ? "*" : item.page + 1));
                return convertView;
            }
        });
    }

    @Override
    public boolean supportDevice() {
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (result) {
            getMenuInflater().inflate(R.menu.bookmarks_menu, menu);
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean showEmptyResult = false;
        switch (item.getItemId()) {
            case R.id.close_bookmarks_menu_item:
                finish();
                return true;
            case R.id.export_bookmarks_menu_item:
                if (bookId == -1) {
                    showEmptyResult = true;
                }

            case R.id.export_all_bookmarks_menu_item:
                String file = getOrionContext().getTempOptions().openedFile;
                if (file == null) {
                    showEmptyResult = true;
                }

                if (!showEmptyResult) {
                    long bookId = item.getItemId() == R.id.export_all_bookmarks_menu_item ? -1 : this.bookId;
                    file = file + "." + (bookId == -1 ? "all_" : "") +  "bookmark.xml";
                    Common.d("Bookmarks output file: " + file);
                    BookmarkExporter exporter = new BookmarkExporter(getOrionContext().getBookmarkAccessor(), file);
                    try {
                        showEmptyResult = !exporter.export(bookId);
                    } catch (IOException e) {
                        Common.d(e);
                        showError(e);
                        return true;
                    }

                }

                if (showEmptyResult) {
                    Toast.makeText(this, "There is nothing to export!", Toast.LENGTH_LONG).show();;
                } else {
                    Toast.makeText(this, "Bookmarks exported to " + file, Toast.LENGTH_LONG).show();
                }
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void showError(Exception e) {
        Toast.makeText(this, "Error " + e.getMessage(), Toast.LENGTH_SHORT).show();;
        System.out.println("Error " + e.getMessage());
    }
}
