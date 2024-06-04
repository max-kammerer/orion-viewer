package universe.constellation.orion.viewer.test.framework

import android.app.Instrumentation
import android.view.View
import androidx.test.espresso.FailureHandler
import androidx.test.espresso.base.DefaultFailureHandler
import org.hamcrest.Matcher
import universe.constellation.orion.viewer.test.espresso.ScreenshotTakingRule

class EspressoFailureHandler(instrumentation: Instrumentation) : FailureHandler {

    private val delegate: FailureHandler = DefaultFailureHandler(instrumentation.targetContext)

    private var counter = 1

    override fun handle(error: Throwable?, viewMatcher: Matcher<View?>?) {
        ScreenshotTakingRule.dump("_f$counter")
        delegate.handle(error, viewMatcher)
    }
}

