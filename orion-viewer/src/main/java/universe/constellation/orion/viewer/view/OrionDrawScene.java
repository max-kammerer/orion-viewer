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
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.LayoutPosition;
import universe.constellation.orion.viewer.OrionImageView;
import universe.constellation.orion.viewer.util.ColorUtil;
import universe.constellation.orion.viewer.util.MoveUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionDrawScene extends View implements OrionImageView {

    public final static int DEFAULT_STATUS_BAR_SIZE = 20;

    public final static int FONT_DELTA = 3;

    public Bitmap bitmap;

    public LayoutPosition info;

    private CountDownLatch latch;

    private ViewDimensionAware dimensionAware;

    //private int counter = 0;

    private float scale = 1.0f;

    private Point startFocus;

    private Point endFocus;

    private boolean enableMoveOnPinchZoom;

    private Paint borderPaint;

    private Paint backgroundPaint;

    private Paint defaultPaint;

    private boolean showStatusBar;

    private boolean drawOffPage;

    private int statusBarHeight = DEFAULT_STATUS_BAR_SIZE;

    private String title = "";

    private int pageCount = 0;

    private boolean inScaling = false;

    private boolean showOffset = true;

    private List<DrawTask> tasks = new ArrayList<>();

    private Rect stuffTempRect = new Rect();

    private ColorStuff stuff;

    public OrionDrawScene(Context context) {
        super(context);
        init(context);
    }

    public OrionDrawScene(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OrionDrawScene(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        stuff = new ColorStuff(context);

        borderPaint = stuff.borderPaint;
        backgroundPaint = stuff.backgroundPaint;
        defaultPaint = stuff.defaultPaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawOffPage) {
            long backgroundStart = System.currentTimeMillis();
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
            Common.d("OrionView: background rendering takes " + 0.001f * (System.currentTimeMillis() - backgroundStart) + " s");
        }

        canvas.save();
        canvas.translate(0f, statusBarHeight);
        if (bitmap != null && !bitmap.isRecycled()) {
            long start = System.currentTimeMillis();
            Common.d("OrionView: drawing bitmap on view...");

            final float myScale = scale;

            if (inScaling) {
                drawBorder(canvas, myScale, true);
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
                drawBorder(canvas, myScale, false);
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s");

            for (DrawTask drawTask : tasks) {
                drawTask.drawOnCanvas(canvas, defaultPaint, null);
            }
        }
        canvas.restore();

        if (showStatusBar) {
            drawStatusBar(canvas);
        }

        if (latch != null) {
            latch.countDown();
        }
    }

    private void drawBorder(Canvas canvas, float myScale, boolean cleanArea) {
        Common.d(cleanArea ? "Draw: clean page area" : "Draw: draw border");

        borderPaint.setColor(cleanArea ? Color.WHITE : Color.BLACK);
        borderPaint.setStyle(cleanArea ? Paint.Style.FILL : Paint.Style.STROKE);

        int left = (int) ((-info.x.offset - startFocus.x) * myScale + (enableMoveOnPinchZoom ? endFocus.x : startFocus.x));
        int top = (int) ((-info.y.offset - startFocus.y) * myScale + (enableMoveOnPinchZoom ? endFocus.y : startFocus.y));

        int right = (int) (left + info.x.pageDimension * myScale);
        int bottom = (int) (top + info.y.pageDimension * myScale);

        canvas.drawRect(left, top, right, bottom, borderPaint);
    }

    private void drawStatusBar(Canvas canvas) {
        int textY = statusBarHeight - 3;
        int sideMargin = 5;

        if (drawOffPage) {
            int color = defaultPaint.getColor();
            defaultPaint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, getWidth(), statusBarHeight, backgroundPaint);
            defaultPaint.setColor(color);
        }

        String textToRender;
        if (info == null) {
            textToRender =  "? /" + pageCount + " ";
        } else {
            textToRender = (showOffset ? "[" + pad(info.x.offset) + ":" + pad(info.y.offset) + "]  " : " ") + (info.pageNumber + 1) + "/" + pageCount + " ";
        }

        float endWidth = defaultPaint.measureText(textToRender);
        float titleEnd = getWidth() - endWidth - sideMargin;
        canvas.drawText(textToRender, titleEnd, textY, defaultPaint);
        String renderTitle = title;
        endWidth = defaultPaint.measureText(renderTitle);
        int count = renderTitle.length();

        titleEnd -= sideMargin;
        if (endWidth > titleEnd && titleEnd > 0 && endWidth > 0) {
            count = (int) (1.f * count / endWidth * titleEnd);
            count -= 3;
            if (count > 0) {
                renderTitle = renderTitle.substring(0, count) + "...";
            } else {
                renderTitle = "...";
            }
        }
        if (count > 0) {
            canvas.drawText(renderTitle, sideMargin, textY, defaultPaint);
        }
    }

    @Override
    public void onNewImage(Bitmap bitmap, LayoutPosition info, CountDownLatch latch) {
        this.bitmap = bitmap;
        this.latch = latch;
        this.info = info;
        //counter++;
    }

    @Override
    public void onNewBook(String title, int pageCount) {
        this.title = title;
        this.pageCount = pageCount;
    }

    public void setDimensionAware(ViewDimensionAware dimensionAware) {
        this.dimensionAware = dimensionAware;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Common.d("OrionView: onSizeChanged " + w + "x" + h);
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            Point renderingSize = getRenderingSize();
            if (dimensionAware != null ) {
                dimensionAware.onDimensionChanged(renderingSize.x, renderingSize.y);
            }
        }
    }

    public void setColorMatrix(float [] colorMatrix) {
        if (colorMatrix != null) {
            ColorMatrix matrix = new ColorMatrix(colorMatrix);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            defaultPaint.setColorFilter(filter);
            borderPaint.setColorFilter(filter);
            stuff.backgroundPaint.setColorFilter(filter);
            super.setBackgroundColor(ColorUtil.transforColor(Color.WHITE, matrix));
        } else {
            defaultPaint.setColorFilter(null);
            borderPaint.setColorFilter(null);
            stuff.backgroundPaint.setColorFilter(null);
            super.setBackgroundColor(Color.WHITE);
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

    public void setShowOffset(boolean showOffset) {
        boolean oldOffset = this.showOffset;
        this.showOffset = showOffset;
        if (showOffset != oldOffset) {
            invalidate();
        }
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

    public void setDrawOffPage(boolean drawOffPage) {
        this.drawOffPage = drawOffPage;
    }

    public void addTask(DrawTask drawTask) {
        tasks.add(drawTask);
    }

    public void removeTask(DrawTask drawTask) {
        tasks.remove(drawTask);
    }

    public Point getRenderingSize() {
        if (showStatusBar) {
            statusBarHeight = DEFAULT_STATUS_BAR_SIZE;
        } else {
            statusBarHeight = 0;
        }
        return new Point(getWidth(), getHeight() - statusBarHeight);
    }

    public Rect getViewCoords() {
        return new Rect(0, statusBarHeight, getWidth(), getHeight());
    }

    private String pad(int value) {
        if (value < 10) {
            return "  " + value;
        } else if (value < 100) {
            return " " + value;
        } else {
            return  "" + value;
        }
    }
}
