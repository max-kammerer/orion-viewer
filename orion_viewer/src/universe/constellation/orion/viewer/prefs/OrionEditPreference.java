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
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import universe.constellation.orion.viewer.R;

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

    public OrionEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initValidator(attrs);
    }

    public OrionEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValidator(attrs);
    }

    public OrionEditPreference(Context context) {
        super(context);
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Pattern.compile(pattern).matcher((String) newValue).matches()){
            return true;
        } else {
            //Toast.makeText(getContext(), "asdadad", Toast.LENGTH_SHORT);
            return false;
        }
    }

    public void initValidator(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference);

        pattern = a.getString(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_pattern);
        a.recycle();
        if (pattern != null) {
            setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        if (originalSummary != null) {
            originalSummary = summary;
        }
        super.setSummary(summary);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
