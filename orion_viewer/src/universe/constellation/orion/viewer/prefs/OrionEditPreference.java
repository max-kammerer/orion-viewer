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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.Toast;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Device;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.R;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * User: mike
 * Date: 25.01.12
 * Time: 12:41
 */
public class OrionEditPreference extends EditTextPreference  implements Preference.OnPreferenceChangeListener {

    private Integer minValue;
    private Integer maxValue;

    private Boolean notEmpty;

    private String pattern;

    private CharSequence originalSummary;

    private boolean isCurrentBookOption;

    public OrionEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public OrionEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OrionEditPreference(Context context) {
        super(context);
    }


    protected void showDialog(Bundle state) {
        super.showDialog(state);

        if (Device.Info.NOOK_CLASSIC) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            getEditText().requestFocus();
        }
    }



    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Pattern.compile(pattern).matcher((String) newValue).matches()){
            setSummary(originalSummary + ": " + newValue);
            return true;
        } else {
            Toast.makeText(getContext(), "Error on ", Toast.LENGTH_SHORT);
            return false;
        }
    }


    private void init(AttributeSet attrs) {
        originalSummary = getSummary();
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference);
        pattern = a.getString(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_pattern);
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_isBook, false);
        a.recycle();
        if (pattern != null) {
            setOnPreferenceChangeListener(this);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (isCurrentBookOption && !restoreValue) {
            //for android 1.5
            restoreValue = true;
        }
        super.onSetInitialValue(restoreValue, defaultValue);
    }

    @Override
    protected boolean persistString(String value) {
        if (isCurrentBookOption) {
            return persistValue(value);
        } else {
            return super.persistString(value);
        }
    }

//    protected boolean persistInt(int value) {
//        if (isCurrentBookOption) {
//            return persistValue(value);
//        } else {
//            return super.persistInt(value);
//        }
//    }

    protected boolean persistValue(String value) {
        LastPageInfo info = ((OrionApplication) getContext().getApplicationContext()).getCurrentBookParameters();
        if (info != null) {
            try {
                Field f = info.getClass().getDeclaredField(getKey());
                Class clazz = f.getType();
                Object resultValue = value;
                if (int.class.equals(clazz)) {
                    resultValue = Integer.valueOf(value);
                }
                f.set(info, resultValue);
                ((OrionApplication)getContext().getApplicationContext()).processBookOptionChange(getKey(), resultValue);
                return true;
            } catch (Exception e) {
                Common.d(e);
            }
        }
        return  false;
    }


    protected int getPersistedInt(int defaultReturnValue) {
        if (isCurrentBookOption) {
            LastPageInfo info = ((OrionApplication) getContext()).getCurrentBookParameters();
            if (info != null) {
                try {
                    Field f = info.getClass().getDeclaredField(getKey());
                    Integer value = (Integer) f.get(info);
                    return value;
                } catch (Exception e) {
                    Common.d(e);
                }
            }
            return defaultReturnValue;
        } else {
            return super.getPersistedInt(defaultReturnValue);
        }
    }


    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (isCurrentBookOption) {
            LastPageInfo info = ((OrionApplication) getContext().getApplicationContext()).getCurrentBookParameters();
            if (info != null) {
                try {
                    Field f = info.getClass().getDeclaredField(getKey());
                    String value = f.get(info).toString();
                    return value;
                } catch (Exception e) {
                    Common.d(e);
                }
            }
            return  defaultReturnValue;
        } else {
            return super.getPersistedString(defaultReturnValue);
        }
    }
}
