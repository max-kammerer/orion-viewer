package universe.constellation.orion.viewer.device;

import java.lang.reflect.Method;

import universe.constellation.orion.viewer.Common;

/**
 * Created by mike on 9/12/14.
 */
public class OnyxUtil {

    public static boolean isEinkDevice() {
        try {
            Class epdControllerClass = Class.forName("com.onyx.android.sdk.device.DeviceInfo");

            Method singletonGetter = epdControllerClass.getDeclaredMethod("singleton");
            Object deviceInfo = singletonGetter.invoke(null);

            Method controllerGetter = epdControllerClass.getDeclaredMethod("getDeviceController");
            Object controller = controllerGetter.invoke(deviceInfo);
            Common.d("Onyx controller is " + controller);

            Method isEInkScreen = controller.getClass().getDeclaredMethod("isEInkScreen");
            Object isEinkResult = isEInkScreen.invoke(controller);
            Common.d("Onyx isEinkResult is " + isEinkResult);

            return ((Boolean) isEinkResult);
        } catch (Exception e) {
            Common.d(e);
        }
        return false;
    }

}
