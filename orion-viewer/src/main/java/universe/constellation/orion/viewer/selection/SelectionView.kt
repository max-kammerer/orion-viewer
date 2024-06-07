package universe.constellation.orion.viewer.selection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SelectionView extends View {

    private Rect oldRect;

    private final Paint paint = new Paint();

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
