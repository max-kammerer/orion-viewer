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
import universe.constellation.orion.viewer.util.MoveUtil;
import universe.constellation.orion.viewer.view.DrawTask;
import universe.constellation.orion.viewer.view.ViewDimensionAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionView extends View implements OrionImageView {

    private final static int DEFAULT_STATUS_BAR_SIZE = 20;

    private final static int FONT_DELTA = 3;

    public Bitmap bitmap;

    public LayoutPosition info;

    private CountDownLatch latch;

    private ViewDimensionAware dimensionAware;

    private int counter = 0;

    private boolean isNightMode = true;

    private float scale = 1.0f;

    private Point startFocus;

    private Point endFocus;

    private boolean enableMoveOnPinchZoom;

    private Paint currentPaint;

    private Paint borderPaint;

    private Paint nightPaint;

    private Paint lightPaint;

    private boolean showStatusBar;

    private int statusBarHeight = DEFAULT_STATUS_BAR_SIZE;

    private String title = "";

    private int pageCount = 0;

    private boolean inScaling = false;

    private boolean showOffset = true;

    private List<DrawTask> tasks = new ArrayList<DrawTask>();

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
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(2);
        borderPaint.setStyle(Paint.Style.STROKE);


        int fontSize = DEFAULT_STATUS_BAR_SIZE - FONT_DELTA;

        nightPaint = new Paint();
        nightPaint.setColorFilter(nightMatrix);
        nightPaint.setTextSize(fontSize);


        lightPaint = new Paint();
        lightPaint.setColor(Color.BLACK);
        lightPaint.setTextSize(fontSize);

        Typeface tf = Typeface.DEFAULT;
        if (tf != null) {
            nightPaint.setTypeface(tf);
            lightPaint.setTypeface(tf);
            nightPaint.setAntiAlias(true);
            lightPaint.setAntiAlias(true);
        }
        currentPaint = lightPaint;
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


        canvas.save();
        canvas.translate(0f, statusBarHeight);
        if (bitmap != null && !bitmap.isRecycled()) {
            long start = System.currentTimeMillis();
            Common.d("OrionView: drawing bitmap on view...");

            final float myScale = scale;

            //do scaling on pinch zoom
            if (inScaling) {
                Common.d("in scaling");
                canvas.save();
                canvas.translate(
                        -MoveUtil.calcOffset(startFocus.x, endFocus.x, myScale, enableMoveOnPinchZoom),
                        -MoveUtil.calcOffset(startFocus.y, endFocus.y, myScale, enableMoveOnPinchZoom));
                canvas.scale(myScale, myScale);
            }

            canvas.drawBitmap(bitmap, 0, 0, currentPaint);

            if (inScaling) {
                Common.d("in scaling 2");
                canvas.restore();

                borderPaint.setColor(isNightMode ? Color.WHITE : Color.BLACK);
                int left = (int) ((-info.x.offset - startFocus.x) * myScale + (enableMoveOnPinchZoom ? endFocus.x : startFocus.x));
                int top = (int) ((-info.y.offset - startFocus.y) * myScale + (enableMoveOnPinchZoom ? endFocus.y : startFocus.y));

                int right = (int) (left + info.x.pageDimension * myScale);
                int bottom = (int) (top + info.y.pageDimension * myScale);
                canvas.drawRect(left, top, right, bottom, borderPaint);
            }

            Common.d("OrionView: bitmap rendering takes " + 0.001f * (System.currentTimeMillis() - start) + " s");

            for (DrawTask drawTask : tasks) {
                drawTask.drawCanvas(canvas, currentPaint);
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

    private void drawStatusBar(Canvas canvas) {
        int textY = statusBarHeight - 3;
        int sideMargin = 5;

        String textToRender;
        int color = currentPaint.getColor();
        currentPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, getWidth(), statusBarHeight, currentPaint);
        currentPaint.setColor(color);

        if (info == null) {
            textToRender =  "? /" + pageCount + " ";
        } else {
            textToRender = (showOffset ? "[" + pad(info.x.offset) + ":" + pad(info.y.offset) + "]  " : " ") + (info.pageNumber + 1) + "/" + pageCount + " ";
        }

        float endWidth = currentPaint.measureText(textToRender);
        float titleEnd = getWidth() - endWidth - sideMargin;
        canvas.drawText(textToRender, titleEnd, textY, currentPaint);
        String renderTitle = title;
        endWidth = currentPaint.measureText(renderTitle);
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
            canvas.drawText(renderTitle, sideMargin, textY, currentPaint);
        }
    }

    @Override
    public void onNewImage(Bitmap bitmap, LayoutPosition info, CountDownLatch latch) {
        this.bitmap = bitmap;
        this.latch = latch;
        this.info = info;
        counter++;
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
            if (dimensionAware != null ) {
                Point renderingSize = getRenderingSize();
                dimensionAware.onDimensionChanged(renderingSize.x, renderingSize.y);
            }
        }
    }

    public boolean isNightMode() {
        return isNightMode;
    }

    public void setNightMode(boolean nightMode) {
        this.isNightMode = nightMode;
        currentPaint = isNightMode ? nightPaint : lightPaint;
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

    public boolean isShowStatusBar() {
        return showStatusBar;
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
