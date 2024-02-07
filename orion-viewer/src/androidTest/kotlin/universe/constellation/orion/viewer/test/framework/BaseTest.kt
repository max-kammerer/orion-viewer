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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.djvu.DjvuDocument
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.DocumentWithCachingImpl
import java.io.File

internal const val MANUAL_DEBUG = false

abstract class BaseTest {

    @JvmField
    @Rule
    val name = TestName()

    @Before
    fun grantPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (BookDescription.SICP.asFile().canRead()) {
            return
        }

        val grant = device.wait(Until.findObject(By.textContains("Grant")), 60000) ?: error("Can't find grant action in warning dialog")
        grant.click()

        val allowField = device.wait(Until.findObject(By.textContains("Allow")), 60000)
        allowField.click()
        assertTrue(device.findObject(By.checkable(true)).isChecked)
        device.pressBack()
        Espresso.onView(ViewMatchers.withId(R.id.view)).check(matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.view)).check(matches(ViewMatchers.isCompletelyDisplayed()))
        assertTrue(BookDescription.SICP.asFile().canRead())
    }

    @Rule
    @JvmField
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    fun dumpBitmap(suffix: String, bitmap: Bitmap) {
        dumpBitmap(name.methodName, suffix, bitmap)
    }

    companion object {
        val testFolder: File = File(Environment.getExternalStorageDirectory(), "Download/orion")

        const val SICP: String = "sicp.pdf"

        const val ALICE: String = "aliceinw.djvu"

        const val DJVU_SPEC: String = "DjVu3Spec.djvu"
    }
}

internal fun isDjvuDocument(document: Document): Boolean {
    val doc = (document as? DocumentWithCachingImpl)?.doc ?: document
    return doc is DjvuDocument
}