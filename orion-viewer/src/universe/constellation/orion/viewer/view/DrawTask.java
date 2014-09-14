package universe.constellation.orion.viewer.view;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * User: mike
 * Date: 23.11.13
 * Time: 22:03
 */
public interface DrawTask {

    public void drawOnCanvas(Canvas canvas, Paint paint, DrawContext drawContext);

}
