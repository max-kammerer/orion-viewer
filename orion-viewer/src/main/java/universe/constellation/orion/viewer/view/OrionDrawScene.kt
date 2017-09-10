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

package universe.constellation.orion.viewer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.LayoutPosition;
import universe.constellation.orion.viewer.OrionScene;
import universe.constellation.orion.viewer.util.MoveUtil;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionDrawScene extends View implements OrionScene {

    public Bitmap bitmap;

    public LayoutPosition info;

    private CountDownLatch latch;

    private ViewDimensionAware dimensionAware;

    private float scale = 1.0f;

    private Point startFocus;

    private Point endFocus;

    private boolean enableMoveOnPinchZoom;

    private Paint borderPaint;

    private Paint defaultPaint;

    private boolean inScaling = false;

    private List<DrawTask> tasks = new ArrayList<>();

    private Rect stuffTempRect = new Rect();

    private boolean inited = false;

    private ColorStuff stuff;

    public OrionDrawScene(Context context) {
        super(context);
    }

    public OrionDrawScene(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrionDrawScene(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(ColorStuff stuff) {
        this.stuff = stuff;
        defaultPaint = stuff.bd.getPaint();
        borderPaint = stuff.borderPaint;
        inited = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!inited) {
            return;
        }

        canvas.save();
        canvas.translate(0f, 0);
        if (bitmap != null && !bitmap.isRecycled()) {
            long start = System.currentTimeMillis();
            Common.d("OrionView: drawing bitmap on view...");

            final float myScale = scale;

            if (inScaling) {
                Common.d("in scaling");
                canvas.save();
                canvas.translate(
                        -MoveUtil.calcOffset(startFocus.x, endFocus.x, myScale, enableMoveOnPinchZoom),
                        -MoveUtil.calcOffset(startFocus.y, endFocus.y, myScale, enableMoveOnPinchZoom));
                canvas.scale(myScale, myScale);
            }

            stuffTempRect.set(
                    info.x.getOccupiedAreaStart(),
                    info.y.getOccupiedAreaStart(),
                    info.x.getOccupiedAreaEnd(),
                    info.y.getOccupiedAreaEnd());

            canvas.drawBitmap(bitmap, stuffTempRect, stuffTempRect, defaultPaint);

            if (inScaling) {
                canvas.restore();
                drawBorder(canvas, myScale);
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s");

            for (DrawTask drawTask : tasks) {
                drawTask.drawOnCanvas(canvas, stuff, null);
            }
        }
        canvas.restore();

        if (latch != null) {
            latch.countDown();
        }
    }

    private void drawBorder(Canvas canvas, float myScale) {
        Common.d("Draw: border");

        int left = (int) ((-info.x.offset - startFocus.x) * myScale + (enableMoveOnPinchZoom ? endFocus.x : startFocus.x));
        int top = (int) ((-info.y.offset - startFocus.y) * myScale + (enableMoveOnPinchZoom ? endFocus.y : startFocus.y));

        int right = (int) (left + info.x.pageDimension * myScale);
        int bottom = (int) (top + info.y.pageDimension * myScale);

        canvas.drawRect(left, top, right, bottom, borderPaint);
    }

    @Override
    public void onNewImage(Bitmap bitmap, LayoutPosition info, CountDownLatch latch) {
        this.bitmap = bitmap;
        this.latch = latch;
        this.info = info;
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
                dimensionAware.onDimensionChanged(getWidth(), getHeight());
            }
        }
    }


    public boolean isDefaultColorMatrix() {
        return defaultPaint.getColorFilter() == null;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void doScale(float scale, Point startFocus, Point endFocus, boolean enableMoveOnPinchZoom) {
        this.scale = scale;
        this.startFocus = startFocus;
        this.endFocus = endFocus;
        this.enableMoveOnPinchZoom = enableMoveOnPinchZoom;
    }

    public void beforeScaling() {
        inScaling = true;
    }

    public void afterScaling() {
        this.inScaling = false;
    }

    public void addTask(DrawTask drawTask) {
        tasks.add(drawTask);
    }

    public void removeTask(DrawTask drawTask) {
        tasks.remove(drawTask);
    }

    @Nullable
    @Override
    public LayoutPosition getInfo() {
        return info;
    }

    @NotNull
    @Override
    public View toView() {
        return this;
    }
}
