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
import android.widget.*;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.OrionBookmarkActivity;
import universe.constellation.orion.viewer.R;


/**
 * User: mike
 * Date: 07.01.12
 * Time: 12:48
 */
public class ActionListActivity extends Activity {

    //private boolean populating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.actions_selection);

        int type = getIntent().getIntExtra("type", 0);
        TextView header = (TextView) findViewById(R.id.actions_header);
        header.setText(type == 0 ? R.string.short_click : type == 1 ? R.string.long_click : R.string.binding_click);
        final int keyCode = getIntent().getIntExtra("keyCode", 0);
        final boolean isLong = getIntent().getBooleanExtra("isLong", false);
        if (type == 2) {
            String name = KeyEventNamer.getKeyName(keyCode);
            header.setText(header.getText().toString() + " " + name + (isLong ? " [long press]" : ""));
        }

        ListView view = (ListView) findViewById(R.id.actionsGroup);
        final Action [] actions = Action.values();
        view.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, Action.values()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView)super.getView(position, convertView, parent);
                view.setText(actions[position].getName());
                return view;
            }
        });

        view.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        int code = getIntent().getIntExtra("code", 0);

        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (action.getCode() == code) {
                view.setItemChecked(i, true);
                //view.setSelection(i);
                break;
            }
        }
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int code = actions[position].getCode();
                Intent result = new Intent();
                result.putExtra("code", code);
                result.putExtra("keyCode", keyCode);
                result.putExtra("isLong", isLong);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
//        RadioGroup group = (RadioGroup) findViewById(R.id.actionsGroup);
//        Action[] actions = Action.values();
//        for (int i = 0; i < actions.length; i++) {
//            Action action = actions[i];
//            RadioButton button = new RadioButton(this);
//            button.setText(getResources().getString(action.getName()));
//            button.setTag(R.attr.actionId, action.getCode());
//            group.addView(button);
//        }
//
//        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                if (!populating) {
//                    RadioButton button = (RadioButton) group.findViewById(group.getCheckedRadioButtonId());
//                    Integer code = (Integer) button.getTag(R.attr.actionId);
//                    Intent result = new Intent();
//                    result.putExtra("code", code);
//                    setResult(Activity.RESULT_OK, result);
//                    finish();
//                }
//            }
//        });
    }


//    @Override
//    protected void onResume() {
//        super.onResume();
//        populating = true;
//        int type = getIntent().getIntExtra("type", 0);
//        TextView header = (TextView) findViewById(R.id.actions_header);
//        header.setText(type == 0 ? R.string.short_click : type == 1 ? R.string.long_click : R.string.binding_click);
//        int code = getIntent().getIntExtra("code", 0);
//        RadioGroup group = (RadioGroup) findViewById(R.id.actionsGroup);
//        int id = group.getChildAt(0).getId();
//        for (int i = 0; i < group.getChildCount(); i++) {
//            RadioButton button = (RadioButton) group.getChildAt(i);
//            Integer buttone_code = (Integer) button.getTag(R.attr.actionId);
//            if (buttone_code == code) {
//                id = group.getChildAt(i).getId();
//                break;
//            }
//        }
//        group.check(id);
//        populating = false;
//    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }
}
