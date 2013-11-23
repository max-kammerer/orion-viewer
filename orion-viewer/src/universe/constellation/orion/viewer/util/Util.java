package universe.constellation.orion.viewer.util;

import android.graphics.RectF;

import java.util.List;

/**
 * User: mike
 * Date: 23.11.13
 * Time: 20:00
 */
public class Util {

    public static void scale(RectF rect, double scale) {
        rect.left *= scale;
        rect.top *= scale;
        rect.right *= scale;
        rect.bottom *= scale;
    }

    public static boolean inRange(List list, int index) {
        return index >= 0 && index < list.size();
    }
}
