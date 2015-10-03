package universe.constellation.orion.viewer.util;

/**
 * Created by mike on 8/7/14.
 */
public class MoveUtil {

    public static float calcOffset(int startPoint, int endPoint, float scale, boolean supportMove) {
        if (supportMove) {
            return (int) ((startPoint) * (scale - 1) + (startPoint - endPoint));
        } else {
            return (int) ((startPoint) * (scale - 1));
        }
    }
}
