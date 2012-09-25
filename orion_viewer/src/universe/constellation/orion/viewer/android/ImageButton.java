/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
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

package universe.constellation.orion.viewer.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 10.01.12
 * Time: 13:26
 */
public class ImageButton extends android.widget.ImageButton {

    private int actionCode;

    private Action action;

    public ImageButton(Context context) {
        super(context);
        initListener(null);
    }

    public ImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initListener(attrs);
    }

    public ImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initListener(attrs);
    }

    public void initListener(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_android_ImageButton);
            actionCode = a.getInt(R.styleable.universe_constellation_orion_viewer_android_ImageButton_actionId, 0);;
            a.recycle();
            action = Action.getAction(actionCode);
            System.out.println("action code button " + action);
            setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    action.doAction(((OrionViewerActivity) getContext()).getController(), (OrionViewerActivity) getContext(), null);
                }
            });
        }
    }
}
