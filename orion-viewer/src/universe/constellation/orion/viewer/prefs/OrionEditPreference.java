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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.Toast;

import org.holoeverywhere.preference.EditTextPreference;
import org.holoeverywhere.preference.Preference;

import java.util.regex.Pattern;

import universe.constellation.orion.viewer.Device;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 25.01.12
 * Time: 12:41
 */
public class OrionEditPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener {

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

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (minValue != null || maxValue != null) {
            if (newValue == null || "".equals(newValue)) {
                Toast.makeText(getContext(), "Value couldn't be empty!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (minValue != null && minValue > Integer.valueOf((String)newValue)) {
            Toast.makeText(getContext(), "New value should be greater or equal than " + minValue, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (maxValue != null && maxValue < Integer.valueOf((String)newValue)) {
            Toast.makeText(getContext(), "New value should be less or equal than " + maxValue, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (pattern != null && !Pattern.compile(pattern).matcher((String) newValue).matches()) {
            Toast.makeText(getContext(), "Couldn't set value: wrong interval!", Toast.LENGTH_SHORT).show();
            return false;
        }

        setSummary(originalSummary + ": " + newValue);

        return true;
    }


    private void init(AttributeSet attrs) {
        originalSummary = getSummary();
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference);
        pattern = a.getString(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_pattern);
        minValue = getIntegerOrNull(a, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_minValue);
        maxValue = getIntegerOrNull(a, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_maxValue);
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_isBook, false);
        a.recycle();
        if (pattern != null || minValue != null || maxValue != null) {
            setOnPreferenceChangeListener(this);
        }
    }

    private Integer getIntegerOrNull(TypedArray array, int id) {
        int UNDEFINED = -10000;
        int value = array.getInt(id, UNDEFINED);
        if (value == UNDEFINED) {
            return null;
        } else {
            return Integer.valueOf(value);
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
}
