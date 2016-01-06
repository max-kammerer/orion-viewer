package universe.constellation.orion.viewer.view;

import android.graphics.Canvas;

/**
 * User: mike
 * Date: 23.11.13
 * Time: 22:03
 */
public interface DrawTask {

    void drawOnCanvas(Canvas canvas, ColorStuff stuff, DrawContext drawContext);

}
