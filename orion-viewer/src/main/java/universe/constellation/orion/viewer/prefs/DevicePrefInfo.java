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
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import universe.constellation.orion.viewer.device.Device;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 24.03.12
 * Time: 13:39
 */
public class DevicePrefInfo extends DialogPreference {
    public DevicePrefInfo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPersistent(false);
        setDialogLayoutResource(R.layout.device_info);
    }

    public DevicePrefInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.device_info);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView tv = view.findViewById(R.id.MANUFACTURER);
        tv.setText(OrionApplication.MANUFACTURER);
        tv = (TextView) view.findViewById(R.id.MODEL);
        tv.setText(OrionApplication.MODEL);
        tv = (TextView) view.findViewById(R.id.DEVICE);
        tv.setText(OrionApplication.DEVICE);
        tv = (TextView) view.findViewById(R.id.HARDWARE);
        tv.setText(OrionApplication.HARDWARE);

//        tv = (TextView) view.findViewById(R.id.MEMORY);
//
//        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
//        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//        activityManager.getMemoryInfo(memoryInfo);
//
//
//        tv.setText("" + memoryInfo.availMem / 1024 / 1024 + " Mb");
    }
}
