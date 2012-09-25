package universe.constellation.orion.viewer.outline;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import universe.constellation.orion.viewer.OrionBookmarkActivity;
import universe.constellation.orion.viewer.prefs.OrionApplication;

public class OutlineActivity extends ListActivity {
	OutlineItem[] items;

    protected void onResume() {
        super.onResume();
        items = ((OrionApplication)getApplicationContext()).getTempOptions().outline;
        setListAdapter(new OutlineAdapter(getLayoutInflater(), items));
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
        Intent result = new Intent();
        result.putExtra(OrionBookmarkActivity.OPEN_PAGE, items[position].page);
        setResult(Activity.RESULT_OK, result);
		finish();
	}

    private  boolean inSettingCV = false;
    @Override
    public void setContentView(int layoutResID) {
        if (!inSettingCV) {
            inSettingCV = true;
            OrionBookmarkActivity.setContentView(this, layoutResID);
        } else {
            super.setContentView(layoutResID);
        }
    }
}
