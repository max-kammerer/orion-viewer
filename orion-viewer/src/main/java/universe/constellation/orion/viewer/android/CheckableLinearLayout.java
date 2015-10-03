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

package universe.constellation.orion.viewer.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * User: mike
 * Date: 06.09.12
 * Time: 14:59
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {

    private Checkable checkbox;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
	}

    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
    	// find checked text view
		int childCount = getChildCount();
		for (int i = 0; i < childCount; ++i) {
			View v = getChildAt(i);
			if (v instanceof Checkable) {
				checkbox = (Checkable)v;
			}
		}
    }

    public boolean isChecked() {
        return checkbox != null ? checkbox.isChecked() : false;
    }

    public void setChecked(boolean checked) {
    	if (checkbox != null) {
    		checkbox.setChecked(checked);
    	}
    }

    public void toggle() {
    	if (checkbox != null) {
    		checkbox.toggle();
    	}
    }
}