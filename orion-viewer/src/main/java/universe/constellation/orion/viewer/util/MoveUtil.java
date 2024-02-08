package universe.constellation.orion.viewer.util;

public class MoveUtil {

    public static float calcOffset(float startPoint, float endPoint, float scale, boolean supportMove) {
        if (supportMove) {
            return (int) ((startPoint) * (scale - 1) + (startPoint - endPoint));
        } else {
            return (int) ((startPoint) * (scale - 1));
        }
    }
}
