package universe.constellation.orion.viewer.android;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.ListView;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.db.BookmarkAccessor;

/**
 * User: mike
 * Date: 25.02.12
 * Time: 16:24
 */
public class BookmarkkList extends OrionViewerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportDevice() {
        return false;
    }
}
