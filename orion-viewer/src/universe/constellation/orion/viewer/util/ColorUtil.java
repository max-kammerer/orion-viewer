package universe.constellation.orion.viewer.util;

import android.graphics.Color;
import android.graphics.ColorMatrix;

/**
 * Created by mike on 8/10/14.
 */
public class ColorUtil {
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

    public static int transforColor(int color, ColorMatrix matrix) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);

        int [] array = new int [4];
        float[] transformation = matrix.getArray();
        for (int i = 0; i < array.length; i++) {
            int shift = i*5;
            array[i] = (int) (r * transformation[shift + 0] + g * transformation[shift + 1] + b * transformation[shift + 2] + a * transformation[shift + 3] + transformation[shift + 4]);
        }
        return Color.argb(array[3], array[0], array[1], array[2]);
    }
}
