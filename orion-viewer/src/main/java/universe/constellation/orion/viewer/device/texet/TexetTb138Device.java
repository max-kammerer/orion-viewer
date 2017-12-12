package universe.constellation.orion.viewer.device.texet;

import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import universe.constellation.orion.viewer.OperationHolder;
import universe.constellation.orion.viewer.device.AndroidDevice;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 *
 */
public class TexetTb138Device extends AndroidDevice {

    public static final int EPD_A2 = 2;
    public static final int EPD_AUTO = 0;
    public static final int EPD_FULL = 1;
    public static final int EPD_NULL = -1;
    public static final int EPD_OED_PART = 10;
    public static final int EPD_PART = 3;

    private final Method theRequestEpdModeMethod;

    public TexetTb138Device() {
        super();
        // public boolean requestEpdMode(int i)
        Method requestEpdModeMethod;
        try {
            requestEpdModeMethod = View.class.getMethod("requestEpdMode", int.class);
            log("request epd method = " + requestEpdModeMethod);
        } catch (NoSuchMethodException e) {
            log("Method requestEpdMode(int) not found", e);
            requestEpdModeMethod = null;
        }
        theRequestEpdModeMethod = requestEpdModeMethod;

    }


    @Override
    public boolean onKeyUp(int keyCode, boolean isLongPress, @NotNull OperationHolder holder) {

        switch (keyCode) {
            // button '>' on the left and the right side
            case 144: //KeyEvent.KEYCODE_NUMPAD_0:
                holder.value = NEXT;
                return true;

            // button '<' on the left and the right side
            case 143: //KeyEvent.KEYCODE_NUM_LOCK:
                holder.value = PREV;
                return true;

            default:
                return super.onKeyUp(keyCode, isLongPress, holder);

        }
    }

    @Override
    public void flushBitmap() {
        if (getActivity().getView() != null) {
            boolean result = requestEpdMode(getActivity().getView().toView(), EPD_FULL);
            log("Invoked requestEpdMode: "+result);
        }

        super.flushBitmap();
    }


    private boolean requestEpdMode(View aView, int aMode) {
        try {
            return (Boolean)theRequestEpdModeMethod.invoke(aView, aMode);
        } catch (Exception e) {
            log("Can't invoke method: "+ theRequestEpdModeMethod, e);
            return false;
        }
    }

}
