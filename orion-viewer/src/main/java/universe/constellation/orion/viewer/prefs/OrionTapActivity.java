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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.R;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 06.01.12
 * Time: 18:03
 */
public class OrionTapActivity extends OrionBaseActivity {

    private View active_view;
    private int index;
    private boolean isLong;
    private int [] [] myCode = new int[9][2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tap);
        TableLayout table = (TableLayout) findViewById(R.id.tap_table);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View layout = row.getChildAt(j);
                layout.setClickable(true);
                layout.setLongClickable(true);
                TextView shortText = (TextView) layout.findViewById(R.id.shortClick);
                TextView longText = (TextView) layout.findViewById(R.id.longClick);

                int shortCode = prefs.getInt(getKey(i, j, false), -1);
                int longCode = prefs.getInt(getKey(i, j, true), -1);
                if (shortCode == -1) {
                    shortCode = getDefaultAction(i, j, false);
                }
                if (longCode == -1) {
                    longCode = getDefaultAction(i, j, true);
                }
                Action saction = Action.getAction(shortCode);
                Action laction = Action.getAction(longCode);
                shortText.setText(getResources().getString(saction.getName()));
                longText.setText(getResources().getString(laction.getName()));
                final int index = i * 3 + j;
                myCode[index][0] = shortCode;
                myCode[index][1] = longCode;
                layout.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        selectAction(v, false, index);
                    }
                });

                layout.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        return selectAction(v, true, index);
                    }
                });
            }
        }
    }

    private boolean selectAction(View view, boolean isLong, int index) {
        Intent intent = new Intent(OrionTapActivity.this, ActionListActivity.class);
        intent.putExtra("code", myCode[index][isLong ? 1 : 0]);
        intent.putExtra("type", isLong ? 1 : 0);
        active_view = view;
        this.isLong = isLong;
        this.index = index;
        startActivityForResult(intent, 1);
        return true;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (active_view != null) {
                TextView view = (TextView) active_view.findViewById(isLong ? R.id.longClick : R.id.shortClick);
                int code = data.getIntExtra("code", 0);
                Action action = Action.getAction(code);
                myCode[index][isLong ? 1 : 0] = action.getCode();
                view.setText(getResources().getString(action.getName()));

                int i = index / 3;
                int j = index % 3;
                log(index + " " + i + " " + j);
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor ed = pref.edit();
                ed.putInt(getKey(i, j, isLong), action.getCode());
                ed.commit();
            }
        }
    }

    public static int getDefaultAction(int row, int column, boolean isLong) {
        if (row == 1 && column == 1) {
            return isLong ? Action.OPTIONS.getCode() : Action.MENU.getCode();
        } else {
            if (2 - row < column) {
                return isLong ? Action.NEXT.getCode() : Action.NEXT.getCode();
            } else {
                return isLong ? Action.PREV.getCode() : Action.PREV.getCode();
            }
        }
    }

    public static String getKey(int i, int j, boolean isLong) {
        return GlobalOptions.TAP_ZONE +(isLong ? "_LONG_CLICK_" :"_SHORT_CLICK_") + i + "_" + j;
    }


    @Override
    public boolean supportDevice() {
        return false;
    }
}
