/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.prefs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.R;


public class ActionListActivity extends OrionBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onOrionCreate(savedInstanceState, R.layout.actions_selection, false);

        int type = getIntent().getIntExtra("type", 0);
        TextView header = findViewById(R.id.actions_header);
        header.setText(type == 0 ? R.string.short_click : type == 1 ? R.string.long_click : R.string.binding_click);
        final int keyCode = getIntent().getIntExtra("keyCode", 0);
        final boolean isLong = getIntent().getBooleanExtra("isLong", false);
        if (type == 2) {
            String name = KeyEventNamer.getKeyName(keyCode);
            header.setText(header.getText().toString() + " " + name + (isLong ? " [long press]" : ""));
        }

        ListView view = findViewById(R.id.actionsGroup);
        final Action [] actions = Action.values();
        view.setAdapter(new ArrayAdapter<Action>(this, android.R.layout.simple_list_item_single_choice, Action.values()) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                CheckedTextView view = (CheckedTextView)super.getView(position, convertView, parent);
                view.setText(actions[position].getNameRes());
                return view;
            }
        });

        view.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        int code = getIntent().getIntExtra("code", 0);

        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (action.code == code) {
                view.setItemChecked(i, true);
                //view.setSelection(i);
                break;
            }
        }
        view.setOnItemClickListener((parent, view1, position, id) -> {
            int code1 = actions[position].code;
            Intent result = new Intent();
            result.putExtra("code", code1);
            result.putExtra("keyCode", keyCode);
            result.putExtra("isLong", isLong);
            setResult(Activity.RESULT_OK, result);
            finish();
        });
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }
}
