package universe.constellation.orion.viewer.device

import universe.constellation.orion.viewer.log

object OnyxUtil {

    val isEinkDevice: Boolean by lazy {
        try {
            val epdControllerClass = Class.forName("com.onyx.android.sdk.device.DeviceInfo")

            val singletonGetter = epdControllerClass.getDeclaredMethod("singleton")
            val deviceInfo = singletonGetter.invoke(null)

            val controllerGetter = epdControllerClass.getDeclaredMethod("getDeviceController")
            val controller = controllerGetter.invoke(deviceInfo)
            log("Onyx controller is $controller")

            val isEInkScreen = controller.javaClass.getDeclaredMethod("isEInkScreen")
            val isEinkResult = isEInkScreen.invoke(controller)
            log("Onyx isEinkResult is $isEinkResult")

            return@lazy isEinkResult as Boolean
        } catch (e: Exception) {
            log(e)
        }
        false
    }

}
