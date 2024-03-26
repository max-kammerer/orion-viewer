package universe.constellation.orion.viewer.test.framework

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.log
import java.io.File

internal const val MANUAL_DEBUG = false
internal const val LONG_TIMEOUT: Long = 10000
internal const val SHORT_TIMEOUT: Long = 1000

abstract class BaseTest(private val forceGrantAction: Boolean = true) {

    @JvmField
    @Rule
    val name = TestName()

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )


    @Before
    fun testStart() {
        log("Starting test: ${name.methodName}" )
    }

    @Before
    fun grantPermissionsAndCheckInvariants() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        //workaround problem "system ui is not responding" problem
        repeat(3) {
            device.findObject(By.textContains("Wait"))?.click()
            if (device.findObject(By.textContains("stopping")) != null) {
                //workaround for: bluetooth keeps stopping
                device.findObject(By.textContains("Close app"))?.click()
            }
            if (device.findObject(By.textContains("has stopped")) != null) {
                //workaround for: ... process has stopped
                device.findObject(By.textContains("OK"))?.click()
            }
        }

        if (forceGrantAction && !BookDescription.SICP.asFile().canRead()) {
            val grant =
                device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT) ?: run {
                    //in case of problem with system UI
                    device.wait(Until.findObject(By.textContains("Wait")), SHORT_TIMEOUT)?.click()
                    device.wait(Until.findObject(By.textContains("Grant")), LONG_TIMEOUT)
                        ?: error("Can't find grant action in warning dialog")
                }

            grant.click()

            val allowField = device.wait(Until.findObject(By.textContains("Allow")), LONG_TIMEOUT)
            allowField.click()
            device.wait(Until.findObject(By.checkable(true)), LONG_TIMEOUT)
            assertTrue(device.findObject(By.checkable(true)).isChecked)
            device.pressBack()
            Espresso.onView(ViewMatchers.withId(R.id.view))
                .check(matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.view))
                .check(matches(ViewMatchers.isCompletelyDisplayed()))
            assertTrue(BookDescription.SICP.asFile().canRead())
        }
    }

    @After
    fun testEnd() {
        log("Finishing test: ${name.methodName}" )
    }

    fun dumpBitmap(suffix: String, bitmap: Bitmap) {
        dumpBitmap(name.methodName, suffix, bitmap)
    }

    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "Download/orion")
        val testDataFolder: File = File(testFolder, "testData")
        val testFailures: File = File(testFolder, "failures")

        const val SICP: String = "sicp.pdf"

        const val ALICE: String = "aliceinw.djvu"

        const val DJVU_SPEC: String = "DjVu3Spec.djvu"
    }
}

internal fun isDjvuDocument(document: Document): Boolean {
    return document is DjvuDocument
}