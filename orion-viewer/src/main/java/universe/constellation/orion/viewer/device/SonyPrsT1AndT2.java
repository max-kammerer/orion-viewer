package universe.constellation.orion.viewer.device;

import android.view.KeyEvent;

import universe.constellation.orion.viewer.OperationHolder;

/**
 * Created by mike on 12/10/15.
 */
public class SonyPrsT1AndT2 extends EInkDeviceWithoutFastRefresh {

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder holder) {
        if (keyCode == 0) {
            switch (event.getScanCode()) {
                case 105:
                    holder.value = PREV;
                    return true;
                case 106:
                    holder.value = NEXT;
                    return true;
            }
        }

        return super.onKeyUp(keyCode, event, holder);
    }
}
