package universe.constellation.orion.viewer.test.framework

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.prefs.OrionApplication

abstract class BaseTestWithActivity(startIntent: Intent) : BaseInstrumentationTest() {

    val globalOptions by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OrionApplication).options
    }

    @get:Rule
    val activityScenarioRule = activityScenarioRule<OrionViewerActivity>(startIntent)

    protected fun awaitBookLoading() {
        activityScenarioRule.scenario.awaitBookLoading()
    }
}

fun BaseInstrumentationTest.doFail(message: String, namePrefix: String = name.methodName): Nothing {
    screenshotRule.takeScreenshot(namePrefix)
    logError(message)
    error(Assert.fail(message))
}

fun BaseTestWithActivity.checkNotEquals(message: String, expected: Int, actual: Int, namePrefix: String = name.methodName) {
    if (expected == actual) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertNotEquals(message, expected, actual)
    }
}

fun BaseTestWithActivity.checkEquals(message: String, expected: Int, actual: Int, namePrefix: String = name.methodName) {
    if (expected != actual) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertEquals(message, expected, actual)
    }
}

fun BaseTestWithActivity.checkTrue(message: String, condition: Boolean, namePrefix: String = name.methodName) {
    if (!condition) {
        screenshotRule.takeScreenshot(namePrefix)
        logError(message)
        Assert.assertTrue(message, condition)
    }
}

fun <T: Any> BaseTestWithActivity.onActivity(body: (OrionViewerActivity) -> T): T {
    return activityScenarioRule.onActivity(body)
}

fun <T: Any> ActivityScenarioRule<OrionViewerActivity>.onActivity(body: (OrionViewerActivity) -> T): T {
    lateinit var res: T
    scenario.onActivity {
        res = body(it)
    }
    return res
}

fun <T: Any> ActivityScenario<OrionViewerActivity>.onActivityRes(body: (OrionViewerActivity) -> T): T {
    lateinit var res: T
    onActivity {
        res = body(it)
    }
    return res
}

fun ActivityScenario<OrionViewerActivity>.awaitBookLoading() {
    lateinit var job: Job
    onActivity {
        job = it.openJob
    }
    runBlocking {
        job.join()
    }
}

val instrumentationContext: Context
    get() = InstrumentationRegistry.getInstrumentation().context

val appContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

