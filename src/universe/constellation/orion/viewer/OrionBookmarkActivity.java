package universe.constellation.orion.viewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.Device;
import com.google.code.orion_viewer.FileChooser;
import com.google.code.orion_viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.db.Bookmark;
import universe.constellation.orion.viewer.db.BookmarkAccessor;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.File;
import java.util.List;

/**
 * User: mike
 * Date: 25.02.12
 * Time: 16:20
 */
public class OrionBookmarkActivity extends OrionBaseActivity {

    public static final String OPEN_PAGE = "open_page";

    public static final String BOOK_ID = "book_id";

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
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        BookmarkAccessor accessor = getOrionContext().getBookmarkAccessor();
        long bookId = intent.getLongExtra(BOOK_ID, -1);
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
}
