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
import android.preference.ListPreference;
import android.util.AttributeSet;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 25.08.12
 * Time: 13:26
 */
public class OrionListPreference extends ListPreference {

    private boolean isCurrentBookOption;

    public OrionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OrionListPreference(Context context) {
        super(context);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference);
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference_isBook, false);
        a.recycle();
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

    protected String getPersistedString(String defaultReturnValue) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedString(this, defaultReturnValue);
        } else {
            return super.getPersistedString(defaultReturnValue);
        }
    }

}
