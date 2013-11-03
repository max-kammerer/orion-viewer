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

package universe.constellation.orion.viewer.android.touch;

import android.view.MotionEvent;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.selection.TouchAutomata;
import universe.constellation.orion.viewer.selection.TouchAutomataOldAndroid;

/**
 * User: mike
 * Date: 06.01.13
 * Time: 10:01
 * Scale wrapper for Android 2.0-2.1
 */
public class OldAdroidScaleWrapper implements ScaleDetectorWrapper {

    private ScaleGestureDetectorOld detector;

    public OldAdroidScaleWrapper(OrionViewerActivity activity, final TouchAutomata automata) {
        detector = new ScaleGestureDetectorOld(activity.getApplicationContext(), new ScaleGestureDetectorOld.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetectorOld detector) {
                System.out.println("onScale " + detector.getScaleFactor());
                return automata.onTouch(null, TouchAutomataOldAndroid.PinchEvents.DO_SCALE, detector.getScaleFactor());
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetectorOld detector) {
                float x = detector.getFocusX();
                float y = detector.getFocusY();
                System.out.println("onScaleBegin " + detector.getScaleFactor());
                System.out.println("focus: " + x + " " + y);
                return automata.onTouch(null, TouchAutomataOldAndroid.PinchEvents.START_SCALE, detector.getScaleFactor());
            }

            @Override
            public void onScaleEnd(ScaleGestureDetectorOld detector) {
                automata.onTouch(null, TouchAutomataOldAndroid.PinchEvents.END_SCALE, detector.getScaleFactor());
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    @Override
    public boolean isInProgress() {
        return detector.isInProgress();
    }

    @Override
    public float getScaleFactor() {
        return detector.getScaleFactor();
    }

    @Override
    public float getFocusX() {
        return detector.getFocusX();
    }

    @Override
    public float getFocusY() {
        return detector.getFocusY();
    }
}
