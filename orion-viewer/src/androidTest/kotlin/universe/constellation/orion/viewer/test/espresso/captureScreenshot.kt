package universe.constellation.orion.viewer.test.espresso

import androidx.test.core.app.takeScreenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import universe.constellation.orion.viewer.test.framework.dumpBitmap
import java.text.SimpleDateFormat
import java.util.Date

private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyMMdd_HH_mm_ss")

class ScreenshotTakingRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        dump(description.methodName.take(15))
    }

    fun takeScreenshot(methodName: String) {
        dump("_" + methodName.take(15))
    }

    companion object {
        fun dump(filePrefix: String) {
            dumpBitmap(filePrefix, simpleDateFormat.format(Date()), takeScreenshot())
        }
    }
}
