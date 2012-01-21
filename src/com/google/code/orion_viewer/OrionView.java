package com.google.code.orion_viewer;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import universe.constellation.orion.viewer.OrionViewerActivity;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 13:52
 */
public class OrionView extends View {

    public Bitmap bitmap;

    private CountDownLatch latch;

    private boolean startRenderer;

    private Controller controller;

    private int counter = 0;

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
            if (counter < options.getEinkRefresafter()) {
                N2.setGL16Mode();
            } else {
                counter = 0;
            }
        }
        //
//        if (!startRenderer && controller != null) {
//            startRenderer = true;
//            controller.startRenderer();
//        }
        if (bitmap != null && !bitmap.isRecycled()) {
            long start = new Date().getTime();
            Common.d("Start drawing bitmap");

//                if (rotation != 0) {
//                    canvas.rotate(-rotation * 90, (getHeight()) / 2, getWidth() / 2);
//                    canvas.translate(- rotation * 80, - rotation * 80);
//                }
//                Paint paint = new Paint();
//                ColorFilter filter = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
//                paint.setColorFilter(filter);
            canvas.drawBitmap(bitmap, 0, 0, null);

            Common.d("OrionView drawn bitmap at " + 0.001f * (new Date().getTime() - start) + " s");
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


//   public void setRotation(int rotation) {
//       synchronized (this) {
//        this.rotation = rotation;
//       }
//    }


    public void setController(Controller controller) {
        startRenderer = false;
        this.controller = controller;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Common.d("onSizeChanged " + w + " " +h);
        //for alex initialization
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != 0 && h != 0 && (w != oldw || h != oldh)) {
            if (controller != null ) {
                controller.screenSizeChanged(w, h);
            }
        }
    }
}
