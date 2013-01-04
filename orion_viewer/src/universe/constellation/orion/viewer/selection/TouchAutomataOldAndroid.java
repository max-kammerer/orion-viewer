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

package universe.constellation.orion.viewer.selection;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.OrionView;
import universe.constellation.orion.viewer.OrionViewerActivity;

/**
 * User: mike
 * Date: 04.01.13
 * Time: 10:22
 */
public class TouchAutomataOldAndroid {

    public enum States {UNDEFINED, SINGLE_CLICK, LONG_CLICK, PINCH_ZOOM, DO_ACTION};

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


    public boolean onTouch(MotionEvent event) {
        boolean processed = false;

        switch (currentState) {
            case UNDEFINED:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startTime = SystemClock.uptimeMillis();
                    start0.x = (int) event.getX();
                    start0.y = (int) event.getY();
                    last0.x = start0.x;
                    last0.y = start0.y;
                    nextState = States.SINGLE_CLICK;
                    processed = true;
                }
                break;

            case SINGLE_CLICK:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        last0.x = start0.x;
                        last0.y = start0.y;
                        processed = true;
                        System.out.println("In action down twice");
                    }

                    if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                        boolean doAction = false;
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            Common.d("UP " + event.getAction());
                            doAction = true;
                        } else {
                            if (last0.x != -1 && last0.y != -1) {
                                boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;
                                doAction = isLongClick;
                            }
                        }

                        if (doAction) {
                            Common.d("Check event action " + event.getAction());
                            boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;

                            if (last0.x != -1 && last0.y != -1) {
                                int width = getView().getWidth();
                                int height = getView().getHeight();

                                int i = 3 * last0.y / height;
                                int j = 3 * last0.x / width;

                                int code = activity.getGlobalOptions().getActionCode(i, j, isLongClick);
                                activity.doAction(code);

                                nextState = States.UNDEFINED;
                            }

                        }
                    }
                break;
        }

        if (nextState != currentState) {
            System.out.println("Next state = " + nextState);
            switch (nextState) {
                case UNDEFINED: reset(); break;
            }
        }
        currentState = nextState;
        return processed;
    }

    public void reset() {
        currentState = States.UNDEFINED;
        prevState = States.UNDEFINED;
        nextState = States.UNDEFINED;
        startTime = 0;
        start0.x = -1;
        start0.y = -1;
    }

}
