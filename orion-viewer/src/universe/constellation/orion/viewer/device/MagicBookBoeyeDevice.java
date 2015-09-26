package universe.constellation.orion.viewer.device;

import android.view.KeyEvent;

import universe.constellation.orion.viewer.OperationHolder;

/**
 * Created by mike on 9/26/15.
 */
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder holder) {
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

        return super.onKeyUp(keyCode, event, holder);
    }
}
