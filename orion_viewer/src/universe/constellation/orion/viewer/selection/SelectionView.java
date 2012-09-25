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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
* User: mike
* Date: 20.08.12
* Time: 22:06
*/
public class SelectionView extends View {

    private Rect oldRect;

    private Paint paint = new Paint();

    public SelectionView(Context context) {
        super(context);
    }

    public SelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setAlpha(128);

        if (oldRect != null) {
            //System.out.println("Draw rect " + oldRect);
            canvas.drawRect(oldRect, paint);
        }
    }

    public void updateView(int left, int top, int right, int bottom) {
        //System.out.println("updateView");
        Rect newRect = new Rect(left, top, right, bottom);
        Rect invalidate = new Rect(newRect);
        if (oldRect != null) {
            invalidate.union(oldRect);
        }
        oldRect = newRect;

        //postInvalidateDelayed(30, invalidate.left, invalidate.top, invalidate.right, invalidate.bottom);
        invalidate(invalidate);
    }

    public void reset() {
        oldRect = null;
    }


}
