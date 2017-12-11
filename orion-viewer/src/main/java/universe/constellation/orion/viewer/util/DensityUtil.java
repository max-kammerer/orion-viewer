package universe.constellation.orion.viewer.util;

import android.content.Context;
import android.util.DisplayMetrics;

import static universe.constellation.orion.viewer.LoggerKt.log;

public class DensityUtil {

    public static double calcScreenSize(int originalSize, DisplayMetrics metrics) {
        log("Device dpi: " + metrics.density);
        return (originalSize * metrics.density + 0.5);
    }

    public static double calcScreenSize(int originalSize, Context activity) {
        return calcScreenSize(originalSize, activity.getResources().getDisplayMetrics());
    }
}
