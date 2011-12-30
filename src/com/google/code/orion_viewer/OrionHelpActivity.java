package com.google.code.orion_viewer;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
* User: mike
* Date: 26.12.11
* Time: 15:08
*/
public class OrionHelpActivity extends OrionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.android_help);
        super.onCreate(savedInstanceState);
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
}
