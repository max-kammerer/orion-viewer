package universe.constellation.orion.viewer.device;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;

import universe.constellation.orion.viewer.OperationHolder;
import universe.constellation.orion.viewer.prefs.OrionApplication;

public class MagicBookBoeyeDevice extends EInkDevice {

    private static final int MENU = 59;
    private static final int F5 = 63;
    private static final int HOME = 102;
    private static final int PAGE_UP = 104;
    private static final int PAGE_DOWN = 109;
    private static final int VOLUME_DOWN = 114;
    private static final int VOLUME_UP = 115;
    private static final int POWER = 115;
    private static final int NOTIFICATION = 143;
    private static final int BACK = 158;
    private static final int CAMERA = 212;
    private static final int SEARCH = 217;

    private static final boolean isT62 = "T62D".equalsIgnoreCase(OrionApplication.DEVICE);

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder holder) {
        if (isT62) {
            switch (keyCode) {
                case PAGE_UP:
                case VOLUME_UP:
                    holder.value = PREV;
                    return true;
                case PAGE_DOWN:
                case VOLUME_DOWN:
                    holder.value = NEXT;
                    return true;
            }
        }

        return super.onKeyUp(keyCode, event, holder);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void fullScreen(boolean on, Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(on ? View.GONE : View.VISIBLE);
    }
}
