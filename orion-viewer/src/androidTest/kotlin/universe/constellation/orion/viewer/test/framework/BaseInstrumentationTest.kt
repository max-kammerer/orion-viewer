package universe.constellation.orion.viewer.test.framework

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule

abstract class BaseInstrumentationTest() : BaseTest() {

    @JvmField
    @Rule
    val screenshotRule = ScreenshotTakingRule()

    protected val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    protected fun processEmulatorErrors() {
        if (!isAtLeastKitkat()) return

        device.waitForIdle()

        repeat(3) {
            if (device.findObject(By.textContains("System UI isn't responding")) != null) {
                device.findObject(By.textContains("Close"))?.click()
            }
            if (device.findObject(By.textContains("stopping")) != null) {
                //workaround for: bluetooth keeps stopping
                device.findObject(By.textContains("Close app"))?.click()
            }
            if (device.findObject(By.textContains("has stopped")) != null) {
                //workaround for: ... process has stopped
                device.findObject(By.textContains("OK"))?.click()
            }
        }
    }
}