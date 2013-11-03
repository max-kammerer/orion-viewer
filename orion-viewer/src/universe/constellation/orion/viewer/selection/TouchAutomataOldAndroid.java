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

package universe.constellation.orion.viewer.selection;

import android.graphics.Point;
import universe.constellation.orion.viewer.OrionView;
import universe.constellation.orion.viewer.OrionViewerActivity;

/**
* User: mike
* Date: 04.01.13
* Time: 10:22
*/
public class TouchAutomataOldAndroid {

    public enum States {UNDEFINED, SINGLE_CLICK, LONG_CLICK, PINCH_ZOOM, DO_MOVE, DO_ACTION};

    public enum PinchEvents {START_SCALE, DO_SCALE, END_SCALE};

    protected static final long TIME_DELTA = 800;

    protected States currentState = States.UNDEFINED;

    protected States prevState = States.UNDEFINED;

    protected States nextState = States.UNDEFINED;

    protected long startTime;

    protected Point start0 = new Point();

    protected Point last0 = new Point();

    protected OrionViewerActivity activity;

    protected OrionView view;

    public TouchAutomataOldAndroid(OrionViewerActivity activity, OrionView view) {
        this.activity = activity;
        this.view = view;
    }

    public OrionView getView() {
        return view;
    }


}
