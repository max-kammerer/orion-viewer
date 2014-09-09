package universe.constellation.orion.viewer.device;

import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import universe.constellation.orion.viewer.Common;

/**
 * Created by mike on 9/9/14.
 */
public class OnyxM96Device extends EInkDevice {

    private static Class epdControllerClass;

    private  static Class epdControllerModeClass;

    private static Object[] updateModeEnum;

    private static Object fastUpdateEntry;

    private static Method invalidate;

    private static final boolean successful;

    static {
        boolean isSuccessful = false;
        try {
            epdControllerClass = Class
                    .forName("com.onyx.android.sdk.device.EpdController");

            epdControllerModeClass = Class
                    .forName("com.onyx.android.sdk.device.EpdController$UpdateMode");

            invalidate = epdControllerClass.getDeclaredMethod("invalidate", android.view.View.class, epdControllerModeClass);

            if (epdControllerModeClass.isEnum()) {
                updateModeEnum = epdControllerModeClass.getEnumConstants();
                for (Object entry : updateModeEnum) {
                    if ("GU".equals(entry.toString())) {
                        fastUpdateEntry = entry;
                    }
                    Common.d("Fast update entry " + entry);
                }
            }

            if (fastUpdateEntry != null) {
                isSuccessful = true;
            }
        } catch (Exception e) {
            Common.d(e);
        } finally {
            successful = isSuccessful;
        }
    }

    @Override
    public void doPartialUpdate(View view) {
        if (successful) {
            try {
                invalidate.invoke(null, view, fastUpdateEntry);
            } catch (IllegalAccessException e) {
                Common.d(e);
                super.doPartialUpdate(view);
            } catch (InvocationTargetException e) {
                Common.d(e);
                super.doPartialUpdate(view);
            }
        } else {
            super.doPartialUpdate(view);
        }
    }


}
