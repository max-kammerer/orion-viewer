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

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.holoeverywhere.preference.DialogPreference;

import universe.constellation.orion.viewer.R;

import java.util.ArrayList;
import java.util.List;


/**
 * User: mike
 * Date: 06.09.12
 * Time: 12:54
 */
public class OrionLayoutDialog extends DialogPreference {

    private boolean isCurrentBookOption;

    private int position = -1;

    public OrionLayoutDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPositiveButtonText(null);
        init(attrs);
    }


    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionLayoutDialog);
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionLayoutDialog_isBook, false);
        a.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        ListView lv = new ListView(getContext());
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OrionLayoutDialog.this.position = position;
                onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                getDialog().dismiss();
            }
        });
        lv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        int[] arr = getContext().getResources().getIntArray(R.array.page_layouts);
        ArrayList values = new ArrayList(arr.length);
        for (int i = 0; i < arr.length; i++) {
            values.add(arr[i]);
        }

        lv.setAdapter(new LayoutAdapter(getContext(), R.layout.page_layout_pref, android.R.id.text1, values));
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        return lv;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (isCurrentBookOption && !restorePersistedValue) {
            //for android 1.5
            restorePersistedValue = true;
        }

        if (restorePersistedValue) {
            position = getPersistedInt(0);
        } else {
            position = (Integer) defaultValue;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        ((ListView)view).setItemChecked(position, true);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && position >= 0) {
            if (callChangeListener(position)) {
                persistInt(position);
            }
        }
    }

    @Override
    protected boolean persistInt(int value) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.persistValue(this, "" + value);
        } else {
            return super.persistInt(value);
        }
    }

    @Override
    protected boolean persistString(String value) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.persistValue(this, value);
        } else {
            return super.persistString(value);
        }
    }

    protected int getPersistedInt(int defaultReturnValue) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedInt(this, defaultReturnValue);
        } else {
            return super.getPersistedInt(defaultReturnValue);
        }
    }


    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedString(this, defaultReturnValue);
        } else {
            return super.getPersistedString(defaultReturnValue);
        }
    }

    public class LayoutAdapter extends ArrayAdapter {

        private int[] images = new int[]{R.drawable.navigation1, R.drawable.navigation2, R.drawable.navigation3};

        public LayoutAdapter(Context context, int res, int textViewResourceId, List objects) {
            super(context, res, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);
            CheckedTextView view = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            view.setText("");
//            ((CheckableLinearLayout)convertView).setChecked(position == OrionLayoutDialog.this.position);
            ImageView button = (ImageView) convertView.findViewById(R.id.ibutton);
            button.setImageResource(images[position]);

            return convertView;
        }
    }

}
