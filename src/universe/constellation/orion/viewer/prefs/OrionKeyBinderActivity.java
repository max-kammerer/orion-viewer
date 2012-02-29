package universe.constellation.orion.viewer.prefs;

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

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.code.orion_viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 27.12.11
 * Time: 14:59
 */
public class OrionKeyBinderActivity extends OrionBaseActivity {

    private boolean binding = false;

    private Button button ;

    private TextView statusText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_binder);

        Button button = (Button) findViewById(R.id.next_bind);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startBinding((Button) view);
            }
        });

        button = (Button) findViewById(R.id.prev_bind);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startBinding((Button) view);
            }
        });

        button = (Button) findViewById(R.id.reset_bind);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(GlobalOptions.NEXT_KEY);
                editor.remove(GlobalOptions.PREV_KEY);
                editor.commit();
                updateButtons();
            }
        });

        statusText = (TextView) findViewById(R.id.press_button);
        statusText.setTextColor(Color.RED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    public void updateButtons() {
        binding = false;
        button = null;

        Button button = (Button) findViewById(R.id.next_bind);
        GlobalOptions options = getOrionContext().getOptions();
        button.setText(options.getNextKey()== - 1 ? "Not binded" : "Binded to " + options.getNextKey());

        button = (Button) findViewById(R.id.prev_bind);
        button.setText(options.getPrevKey()== -1 ? "Not binded" : "Binded to " + options.getPrevKey());

        statusText.setText("");
    }


    @Override
    protected void onPause() {
        super.onPause();
        binding = false;
        button = null;
    }

    public void startBinding(Button b) {
        button = b;
        binding = true;
        statusText.setText("Press eny key!");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (binding) {
            if (keyCode != KeyEvent.KEYCODE_MENU) {
                button.setText("Binded to " + keyCode);
                statusText.setText("Key successfully binded!");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(button.getId() == R.id.next_bind ? GlobalOptions.NEXT_KEY : GlobalOptions.PREV_KEY, keyCode);
                editor.commit();
            } else {
                statusText.setText("You cannot bind Menu button!!!");
            }
            binding = false;
            button = null;
            return true;
        }

        return super.onKeyDown(keyCode, event);
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
