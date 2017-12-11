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
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.Toast;

import universe.constellation.orion.viewer.device.Device;
import universe.constellation.orion.viewer.LayoutPosition;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.android.touch.AndroidScaleWrapper;
import universe.constellation.orion.viewer.android.touch.OldAdroidScaleWrapper;
import universe.constellation.orion.viewer.android.touch.ScaleDetectorWrapper;
import universe.constellation.orion.viewer.device.EInkDevice;
import universe.constellation.orion.viewer.prefs.OrionApplication;
import universe.constellation.orion.viewer.util.DensityUtil;
import universe.constellation.orion.viewer.util.MoveUtil;
import universe.constellation.orion.viewer.view.OrionDrawScene;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static universe.constellation.orion.viewer.LoggerKt.log;

/**
* User: mike
* Date: 02.01.13
* Time: 18:57
*/
public class TouchAutomata extends TouchAutomataOldAndroid {

    private final int MOVE_THRESHOLD;

    private final boolean enableTouchMove;

    private final boolean enableTouchMoveOnPinchZoom;

    private Point startFocus = new Point();

    private Point endFocus = new Point();

    private float curScale = 1f;
    private LayoutPosition info;

    private ScaleDetectorWrapper gestureDetector;

    public TouchAutomata(OrionViewerActivity activity, OrionDrawScene view) {
        super(activity, view);
        int sdkVersion = activity.getOrionContext().getSdkVersion();
        gestureDetector = sdkVersion >= 8 ? new AndroidScaleWrapper(activity, this) : new OldAdroidScaleWrapper(activity, this);
        double edgeSize = DensityUtil.calcScreenSize(40, activity); //40 px * density factor
        MOVE_THRESHOLD = (int) (edgeSize * edgeSize);

        enableTouchMove = activity.getGlobalOptions().isEnableTouchMove();
        enableTouchMoveOnPinchZoom = activity.getGlobalOptions().isEnableMoveOnPinchZoom();
    }

    public void startAutomata() {
        reset();
    }

    public boolean onTouch(MotionEvent event) {
        return onTouch(event, null, 0);
    }

    public boolean onTouch(MotionEvent event, PinchEvents pinch, float scale) {
        boolean processed = false;

        if (pinch == null && gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
            if (gestureDetector.isInProgress()) {
                return true;
            }
        }

        int action;
        if (event != null) { //in case of scale
            last0.x = (int) event.getX();
            last0.y = (int) event.getY();
            action = event.getAction();
        } else {
            action = -100;
        }

        OrionDrawScene view = getView();
        switch (currentState) {
            case UNDEFINED:
                if (PinchEvents.START_SCALE == pinch) {
                    nextState = States.PINCH_ZOOM;
                    processed = true;
                } else if (ACTION_DOWN == action) {
                    startTime = SystemClock.uptimeMillis();
                    start0.x = (int) event.getX();
                    start0.y = (int) event.getY();
                    nextState = States.SINGLE_CLICK;
                    processed = true;
                }
                break;

            case DO_MOVE:
                if (pinch != null) {
                    nextState = States.PINCH_ZOOM;
                } else  if (ACTION_MOVE == action || ACTION_UP == action) {
                    int width = view.getWidth();
                    if (insideViewWidth(view.getInfo())) {
                        last0.x = start0.x;
                    } else {
                        int delta = last0.x - start0.x;
                        int offset = -info.x.offset;
                        if (delta < 0) {
                            if (offset + info.x.pageDimension + delta < width) {
                                last0.x = start0.x - offset - info.x.pageDimension + width;
                            }
                        } else {
                            if (offset + delta > 0) {
                                last0.x = start0.x - offset;
                            }
                        }
                    }
                    if (action == ACTION_UP) {
                        view.afterScaling();
                        activity.getController().translateAndZoom(false, 1f, -last0.x + start0.x, -last0.y + start0.y);
                        nextState = States.UNDEFINED;
                    } else {
                        view.beforeScaling();
                        view.doScale(1f, start0, last0, true);
                        view.postInvalidate();
                    }
                    processed = true;
                }

                break;

            case DO_LIGHTING:
                if (pinch != null) {
                    nextState = States.PINCH_ZOOM;
                } else  if (ACTION_MOVE == action || ACTION_UP == action) {
                    int delta = -(last0.y - start0.y);
                    if (ACTION_UP == action) {
                        doLighting(delta);
                        nextState = States.UNDEFINED;
                    } else {
                        doLighting(delta);
                        start0.x = last0.x;
                        start0.y = last0.y;
                    }
                    processed = true;
                }

                break;


            case SINGLE_CLICK:
                if (PinchEvents.START_SCALE == pinch) {
                    nextState = States.PINCH_ZOOM;
                    processed = true;
                } else {
                    if (ACTION_DOWN == action) {
                        last0.x = start0.x;
                        last0.y = start0.y;
                        processed = true;
                        System.out.println("In action down twice");
                    }

                    if (ACTION_MOVE == action && enableTouchMove) {
                        int x = last0.x - start0.x;
                        int y = last0.y - start0.y;
                        System.out.println("move " + (x*x + y*y));
                        if (x*x + y*y >= MOVE_THRESHOLD) {
                            if (isSupportLighting() && isRightHandSide(start0.x) && isRightHandSide(last0.x)) {
                                nextState = States.DO_LIGHTING;
                            } else {
                                nextState = States.DO_MOVE;
                            }
                            break;
                        }
                    }

                    if (ACTION_MOVE == action || ACTION_UP == action) {
                        boolean doAction = false;
                        if (action == ACTION_UP) {
                            log("UP " + action);
                            doAction = true;
                        } else {
                            if (last0.x != -1 && last0.y != -1) {
                                boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;
                                doAction = isLongClick;
                            }
                        }

                        if (doAction) {
                            log("Check event action " + action);
                            boolean isLongClick = (SystemClock.uptimeMillis() - startTime) > TIME_DELTA;

                            if (last0.x != -1 && last0.y != -1) {
                                int width = view.getWidth();
                                int height = view.getHeight();

                                int i = 3 * last0.y / height;
                                int j = 3 * last0.x / width;

                                int code = activity.getGlobalOptions().getActionCode(i, j, isLongClick);
                                activity.doAction(code);

                                nextState = States.UNDEFINED;
                            }

                        }
                    }

                }
                break;

            case PINCH_ZOOM:
                if (pinch != null) {
                    switch (pinch) {
                        case START_SCALE:
                            curScale = gestureDetector.getScaleFactor();
                            startFocus.x = (int) gestureDetector.getFocusX();
                            startFocus.y = (int) gestureDetector.getFocusY();
                            break;
                        case DO_SCALE:
                            curScale *= gestureDetector.getScaleFactor();
                            endFocus.x = (int) gestureDetector.getFocusX();
                            endFocus.y = (int) gestureDetector.getFocusY();
                            view.beforeScaling();
                            view.doScale(curScale, startFocus, endFocus, enableTouchMoveOnPinchZoom);
                            view.postInvalidate();
                            //System.out.println(endFocus.x + " onscale " + endFocus.y);
                            break;
                        case END_SCALE:
                            nextState = States.UNDEFINED;
                            float newX = MoveUtil.calcOffset(startFocus.x, endFocus.x, curScale, enableTouchMoveOnPinchZoom);
                            float newY = MoveUtil.calcOffset(startFocus.y, endFocus.y, curScale, enableTouchMoveOnPinchZoom);
                            view.afterScaling();
                            //There is no start scale event!!!!
                            if (OrionApplication.TEXET_TB176FL) {
                                curScale *= gestureDetector.getScaleFactor();
                            }
                            activity.getController().translateAndZoom(true, curScale, newX, newY);
                            break;
                    }
                } else {
                    nextState = States.UNDEFINED;
                }
                processed = true;
                break;
        }

        if (nextState != currentState) {
            System.out.println("Next state = " + nextState + " oldState = " + currentState);
            switch (nextState) {
                case UNDEFINED: reset(); break;
                case PINCH_ZOOM:
                    curScale = gestureDetector.getScaleFactor();
                    startFocus.x = (int) gestureDetector.getFocusX();
                    startFocus.y = (int) gestureDetector.getFocusY();
                    endFocus.x = startFocus.x;
                    endFocus.y = startFocus.y;
                    processed = true;
                    break;
                case DO_MOVE: info = view.getInfo().clone();
            }
        }
        currentState = nextState;
        return processed;
    }

    public void reset() {
        info = null;
        curScale = 1f;
        currentState = States.UNDEFINED;
        prevState = States.UNDEFINED;
        nextState = States.UNDEFINED;
        startTime = 0;
        start0.x = -1;
        start0.y = -1;
    }

    private boolean isRightHandSide(int x) {
        return view.getWidth() - x < 75;
    }

    private boolean isSupportLighting() {
        Device device = activity.getDevice();
        return device instanceof EInkDevice && ((EInkDevice) device).isLightingSupported();
    }

    private Toast toast;

    private void doLighting(int delta) {
        if (toast == null) {
            toast = Toast.makeText(activity, "-1", Toast.LENGTH_SHORT);
        }

        Device device = activity.getDevice();
        if (device instanceof EInkDevice) {
            try {
                int newBrightness = ((EInkDevice) device).doLighting(delta / 5);
                if (false) {
                    toast.setText("" + newBrightness);
                    toast.show();
                }
            } catch (Exception e) {
                toast.setText("Error " + e.getMessage() + " " + e.getCause());
                toast.show();
                log(e);
            }
        }
    }

    private boolean insideViewWidth(LayoutPosition info) {
        return info != null && info.x.pageDimension <= getView().getWidth();
    }
}
