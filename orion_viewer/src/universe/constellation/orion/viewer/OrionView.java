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

package universe.constellation.orion.viewer;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import universe.constellation.orion.viewer.device.Nook2Util;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionView extends View {

    private static final float DEFAULT_SCALE = 1.0f;

    public Bitmap bitmap;

    private CountDownLatch latch;

    private Controller controller;

    private int counter = 0;

    private boolean isNightMode = true;

    private float scale = 1.0f;

    private Point startFocus;

    private Point endFocus;

    private ColorMatrixColorFilter nightMatrix = new ColorMatrixColorFilter(new ColorMatrix(
            new float[]{
                    -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            }));

    public OrionView(Context context) {
        super(context);
    }

    public OrionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        GlobalOptions options = ((OrionViewerActivity)getContext()).getGlobalOptions();
        if (options.isEinkOptimization()) {
            if (counter < options.getEinkRefreshAfter()) {
                Nook2Util.setGL16Mode();
            } else {
                counter = 0;
            }
        }

        if (bitmap != null && !bitmap.isRecycled()) {
            long start = System.currentTimeMillis();
            Common.d("OrionView: drawing bitmap on view...");

            Paint paint = null;
            if (isNightMode) {
                paint = new Paint();
                paint.setColorFilter(nightMatrix);
            }

            final float myScale = scale;

            //do scaling on pinch zoom
            if (myScale != DEFAULT_SCALE) {
                canvas.save();
                canvas.translate((startFocus.x) * (1 - myScale) - startFocus.x + endFocus.x, (startFocus.y) * (1 - myScale) - startFocus.y + endFocus.y);
                canvas.scale(myScale, myScale);
            }

            canvas.drawBitmap(bitmap, 0, 0, paint);

            if (myScale != DEFAULT_SCALE) {
                canvas.restore();
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s");
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    public void setData(Bitmap bitmap, CountDownLatch latch) {
        this.bitmap = bitmap;
        this.latch = latch;
        counter++;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Common.d("OrionView: onSizeChanged " + w + "x" + h);
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != 0 && h != 0 && (w != oldw || h != oldh)) {
            if (controller != null ) {
                controller.screenSizeChanged(w, h);
            }
        }
    }

    public boolean isNightMode() {
        return isNightMode;
    }

    public void setNightMode(boolean nightMode) {
        this.isNightMode = nightMode;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void doScale(float scale, Point startFocus, Point endFocus) {
        this.scale = scale;
        this.startFocus = startFocus;
        this.endFocus = endFocus;
    }
}
