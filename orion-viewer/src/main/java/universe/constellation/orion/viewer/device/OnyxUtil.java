package universe.constellation.orion.viewer.device;

import java.lang.reflect.Method;

import static universe.constellation.orion.viewer.LoggerKt.log;

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
            log("Onyx controller is " + controller);

            Method isEInkScreen = controller.getClass().getDeclaredMethod("isEInkScreen");
            Object isEinkResult = isEInkScreen.invoke(controller);
            log("Onyx isEinkResult is " + isEinkResult);

            return ((Boolean) isEinkResult);
        } catch (Exception e) {
            log(e);
        }
        return false;
    }

}
