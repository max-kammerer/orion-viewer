package universe.constellation.orion.viewer.util;

import android.content.Context;
import android.util.DisplayMetrics;

import universe.constellation.orion.viewer.Common;

public class DensityUtil {

    public static double calcScreenSize(int originalSize, DisplayMetrics metrics) {
        Common.d("Device dpi: " + metrics.density);
        return (originalSize * metrics.density + 0.5);
    }

    public static double calcScreenSize(int originalSize, Context activity) {
        return calcScreenSize(originalSize, activity.getResources().getDisplayMetrics());
    }
}
