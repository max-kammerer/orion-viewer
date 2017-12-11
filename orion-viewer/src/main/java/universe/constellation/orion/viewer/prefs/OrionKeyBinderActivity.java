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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import universe.constellation.orion.viewer.*;

import java.util.*;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 27.12.11
 * Time: 14:59
 */
public class OrionKeyBinderActivity extends OrionBaseActivity {

    private TextView statusText;

    private ListView bindedKeys;

    private int defaultColor;

    private KeyListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_binder);

        Button button = (Button) findViewById(R.id.reset_bind);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            getOrionContext().getKeyBinding().removeAll();
            adapter.clear();
            }
        });

        statusText = (TextView) findMyViewById(R.id.key_binder_message);
        defaultColor = statusText.getTextColors().getDefaultColor();
        bindedKeys = (ListView) findMyViewById(R.id.binded_keys);
        Map<String, Integer> props = (Map<String, Integer>) getOrionContext().getKeyBinding().getAllProperties();
        adapter = new KeyListAdapter(this, props);
        bindedKeys.setAdapter(adapter);
        bindedKeys.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                KeyCodeAndAction keyCodeAndAction = (KeyCodeAndAction) parent.getAdapter().getItem(position);
                selectAction(keyCodeAndAction.keyCode, keyCodeAndAction.isLong);
            }
        });

        bindedKeys.setFocusable(false);
        button.setFocusable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    public void updateButtons() {
//        binding = false;
//        button = null;
//
//        Button button = (Button) findViewById(R.id.next_bind);
//        GlobalOptions options = getOrionContext().getOptions();
//        button.setText(options.getNextKey()== - 1 ? "Not binded" : "Binded to " + options.getNextKey());
//
//        button = (Button) findViewById(R.id.prev_bind);
//        button.setText(options.getPrevKey()== -1 ? "Not binded" : "Binded to " + options.getPrevKey());
//
//        statusText.setText("");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            int code = data.getIntExtra("code", 0);
            int keyCode = data.getIntExtra("keyCode", 0);
            boolean isLong = data.getBooleanExtra("isLong", false);
            String prefKey = UtilKt.getPrefKey(keyCode, isLong);
            Action action = Action.getAction(code);
            if (action == Action.NONE) {
                getOrionContext().getKeyBinding().removePreference(prefKey);
                adapter.remove(new KeyCodeAndAction(keyCode, action, isLong));
            } else {
                getOrionContext().getKeyBinding().putIntPreference(prefKey, code);
                adapter.insertOrUpdate(new KeyCodeAndAction(keyCode, action, isLong));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (doTrack(keyCode)) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isCanceled()) {
            return super.onKeyUp(keyCode, event);
        }

        return processKey(keyCode, event, false) || super.onKeyUp(keyCode, event);
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return processKey(keyCode, event, true);
    }

    private boolean processKey(int keyCode, KeyEvent event, boolean isLong) {
        //TODO add zero keycode warning
        log("KeyBinder: on key down " + keyCode + (isLong ? " long press" : ""));
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            statusText.setText(R.string.key_binder_warning);
            statusText.setTextColor(Color.RED);
        } else if (keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_BACK) {
            statusText.setText(R.string.key_binder_message);
            statusText.setTextColor(defaultColor);
            selectAction(event.getKeyCode(), isLong);
            return true;
        }
        return false;
    }

    private void selectAction(int keyCode, boolean isLong) {
        Intent intent = new Intent(this, ActionListActivity.class);
        intent.putExtra("code", getOrionContext().getKeyBinding().getInt(UtilKt.getPrefKey(keyCode, isLong), 0));
        intent.putExtra("type", 2);
        intent.putExtra("keyCode", keyCode);
        intent.putExtra("isLong", isLong);
        startActivityForResult(intent, 1);
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

    class KeyCodeAndAction implements Comparable<KeyCodeAndAction>{
        private int keyCode;
        private Action action;
        private boolean isLong;

        KeyCodeAndAction(int keyCode, Action action, boolean isLong) {
            this.keyCode = keyCode;
            this.action = action;
            this.isLong = isLong;
        }

        public String toString() {
            return KeyEventNamer.getKeyName(keyCode) + (isLong ? " [long press]" : "");
        }

        public int compareTo(KeyCodeAndAction object2) {
            return keyCode > object2.keyCode ? 1 : (keyCode < object2.keyCode ? -1 : isLong == object2.isLong ? 0 : isLong ? 1 : -1);
        }
    }

    private class KeyListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public ArrayList<KeyCodeAndAction> values = new ArrayList<KeyCodeAndAction>();

        KeyListAdapter(Context context, Map<String, Integer> prefs) {
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (prefs != null) {
                for (Map.Entry<String, Integer> next : prefs.entrySet()) {
                    String key = next.getKey();
                    Integer actionCode = next.getValue();
                    Action action = Action.getAction(actionCode);
                    if (action != Action.NONE) {
                        try {
                            boolean isLong = key.endsWith("long");
                            if (isLong) {
                                key = key.substring(0, key.length() - "long".length());
                            }
                            values.add(new KeyCodeAndAction(Integer.valueOf(key), action, isLong));
                        } catch (Exception e) {
                            log(e);
                        }
                    }
                }
                Collections.sort(values);
            }
        }

        public int getCount() {
            return values.size();
        }

        public KeyCodeAndAction getItem(int position) {
            return values.get(position);
        }

        public long getItemId(int position) {
            return position;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.two_line_list_item, parent, false);
            }
            KeyCodeAndAction item = getItem(position);
            TextView action = (TextView)convertView.findViewById(android.R.id.text2);
            action.setText(item.action.getName());

            TextView code = (TextView)convertView.findViewById(android.R.id.text1);
            code.setText(KeyEventNamer.getKeyName(item.keyCode) + (item.isLong ? " [long press]" : ""));
            return convertView;
        }

        void insertOrUpdate(KeyCodeAndAction action) {
            int index = Collections.binarySearch(values, action);
            if (index >= 0) {
                values.set(index, action);
            } else {
                index = -index - 1;
                values.add(index, action);
            }
            notifyDataSetChanged();
        }

        public void remove(KeyCodeAndAction action) {
            int index = Collections.binarySearch(values, action);
            if (index >= 0) {
                values.remove(index);
            }
            notifyDataSetChanged();
        }

        public void clear() {
            values.clear();
            notifyDataSetChanged();
        }
    }
}
