package universe.constellation.orion.viewer;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.OrionBaseActivity;

/**
* User: mike
* Date: 26.12.11
* Time: 15:08
*/
public class OrionHelpActivity extends OrionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Common.createDevice().getHelpLayoutId());
        initHelpScreen();
    }

    @Override
    protected void onAnimatorCancel() {
        finish();
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
    public boolean supportDevice() {
        return false;
    }
}
