package universe.constellation.orion.viewer.device;

import android.view.KeyEvent;
import android.view.View;

import universe.constellation.orion.viewer.OperationHolder;

/**
 * Created by mike on 9/9/14.
 */
public class Nook2Device extends EInkDevice {

    protected static final int NOOK_PAGE_UP_KEY_RIGHT = 95;

    protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 94;

    protected static final int NOOK_PAGE_UP_KEY_LEFT = 93;

    protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 92;

    @Override
    public void doPartialUpdate(View view) {
        Nook2Util.setGL16Mode(activity);
        super.doPartialUpdate(view);
    }

    @Override
    public void doFullUpdate(View view) {
        Nook2Util.setFullUpdate(activity);
        super.doFullUpdate(view);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder holder) {
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
                holder.value = PREV;
                return true;
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
                holder.value = NEXT;
                return true;
        }

        return super.onKeyUp(keyCode, event, holder);
    }
}
