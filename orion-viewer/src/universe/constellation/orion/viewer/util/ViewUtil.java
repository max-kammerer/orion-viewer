package universe.constellation.orion.viewer.util;

/**
 * Created by mike on 8/10/14.
 */
public class ViewUtil {
    private static final float[][] COLOR_MATRICES = {
            null, /* COLOR_MODE_NORMAL */
            {
                    -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, /* COLOR_MODE_INVERT */
                    0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f},

            {
                    0.94f, 0.02f, 0.02f, 0.0f, 0.0f, /* COLOR_MODE_BLACK_ON_YELLOWISH */
                    0.02f, 0.86f, 0.02f, 0.0f, 0.0f,
                    0.02f, 0.02f, 0.74f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f},

            {
                    0.25f, 0.54f, 0.06f, 0.0f, 0.0f, /* COLOR_MODE_BLACK_ON_LIGHT_GRAY */
                    0.25f, 0.54f, 0.06f, 0.0f, 0.0f,
                    0.25f, 0.54f, 0.06f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f},

            {
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f, /* COLOR_MODE_GREEN_ON_BLACK */
                    -0.3f, -0.59f, -0.11f, 0.0f, 255.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f},

            {
                    -0.3f, -0.59f, -0.11f, 0.0f, 255.0f, /* COLOR_MODE_RED_ON_BLACK */
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 255.0f}
    };

    public static float[] getColorMode(String type) {
        int index = 0;
        if (type == null || "".equals(type) || "CM_NORMAL".equals(type)) {
            index = 0;
        } else if ("CM_INVERTED".equals(type)) {
            index = 1;
        } else if ("CM_BLACK_ON_YELLOWISH".equals(type)) {
            index = 2;
        } else if ("CM_BLACK_ON_LUGHT_GRAY".equals(type)) {
            index = 4;
        } else if ("CM_GREEN_ON_BLACK".equals(type)) {
            index = 4;
        } else if ("CM_RED_ON_BLACK".equals(type)) {
            index = 5;
        }

        return COLOR_MATRICES[index];
    }
}
