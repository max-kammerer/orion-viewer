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

package universe.constellation.orion.viewer;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import universe.constellation.orion.viewer.device.Nook2Util;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.view.ViewDimensionAware;

import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionView extends View implements OrionImageView {

    private static final float DEFAULT_SCALE = 1.0f;

    public Bitmap bitmap;

    public LayoutPosition info;

    private CountDownLatch latch;

    private ViewDimensionAware dimensionAware;

    private int counter = 0;

    private boolean isNightMode = true;

    private float scale = 1.0f;

    private Point startFocus;

    private Point endFocus;

    private Paint borderPaint;

    private Paint nightPaint;

    private boolean showStatusBar;

    private int statusBarHeight = 10;

    private ColorMatrixColorFilter nightMatrix = new ColorMatrixColorFilter(new ColorMatrix(
            new float[]{
                    -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            }));

    public OrionView(Context context) {
        super(context);
        init();
    }

    public OrionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OrionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(0,0,0));
        borderPaint.setStrokeWidth(2);
        borderPaint.setStyle(Paint.Style.STROKE);

        nightPaint = new Paint();
        nightPaint.setColorFilter(nightMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        GlobalOptions options = ((OrionViewerActivity)getContext()).getGlobalOptions();
        if (options.isEinkOptimization()) {
            if (counter < options.getEinkRefreshAfter()) {
                Nook2Util.setGL16Mode((Activity)getContext());
            } else {
                counter = 0;
            }
        }

        if (bitmap != null && !bitmap.isRecycled()) {
            long start = System.currentTimeMillis();
            Common.d("OrionView: drawing bitmap on view...");

            final float myScale = scale;

            //do scaling on pinch zoom
            if (myScale != DEFAULT_SCALE) {
                canvas.save();
                canvas.translate((startFocus.x) * (1 - myScale) - startFocus.x + endFocus.x, (startFocus.y) * (1 - myScale) - startFocus.y + endFocus.y);
                canvas.scale(myScale, myScale);
            }

            canvas.drawBitmap(bitmap, 0, statusBarHeight, isNightMode ? nightPaint : null);

            if (myScale != DEFAULT_SCALE) {
                canvas.restore();

                borderPaint.setColor(isNightMode ? Color.WHITE : Color.BLACK);
                int left = (int) ((-info.x.offset - startFocus.x) * myScale + endFocus.x);
                int top = (int) ((-info.y.offset - startFocus.y) * myScale + endFocus.y);

                int right = (int) (left + info.x.pageDimension * myScale);
                int bottom = (int) (top + info.y.pageDimension * myScale);
                canvas.drawRect(left, top, right, bottom, borderPaint);
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s");
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onNewImage(Bitmap bitmap, LayoutPosition info, CountDownLatch latch) {
        this.bitmap = bitmap;
        this.latch = latch;
        this.info = info;
        counter++;
    }

    public void setDimensionAware(ViewDimensionAware dimensionAware) {
        this.dimensionAware = dimensionAware;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Common.d("OrionView: onSizeChanged " + w + "x" + h);
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            if (dimensionAware != null ) {
                dimensionAware.onDimensionChanged(w, h);
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

    public boolean isShowStatusBar() {
        return showStatusBar;
    }

    public void setShowStatusBar(boolean showStatusBar) {
        if (showStatusBar != this.showStatusBar && dimensionAware  != null) {
            this.showStatusBar = showStatusBar;
            Point renderingSize = getRenderingSize();
            dimensionAware.onDimensionChanged(renderingSize.x, renderingSize.y);
        } else {
            this.showStatusBar = showStatusBar;
        }
    }

    public Point getRenderingSize() {
        if (showStatusBar) {
            statusBarHeight = 10;
        } else {
            statusBarHeight = 0;
        }
        return new Point(getWidth(), getHeight() - statusBarHeight);
    }
}
