package universe.constellation.orion.viewer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by mike on 11/23/15.
 */
public class Permissions {
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 111;

    public static void checkReadPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    public static void checkPermission(Activity activity, String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            int hasPermission = activity.checkSelfPermission(permission);
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{permission}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }
}
