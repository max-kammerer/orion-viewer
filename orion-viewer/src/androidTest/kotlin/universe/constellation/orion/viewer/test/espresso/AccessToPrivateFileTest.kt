package universe.constellation.orion.viewer.test.espresso

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.test.BuildConfig
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.InstrumentationTestCase
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.SHORT_TIMEOUT
import universe.constellation.orion.viewer.test.framework.failWithScreenShot
import universe.constellation.orion.viewer.test.framework.instrumentationContext
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.view.OrionDrawScene

@SdkSuppress(minSdkVersion = KITKAT)
@RunWith(Parameterized::class)
class AccessToPrivateFileTest(val bookDesc: BookFile) :
    InstrumentationTestCase(bookDesc.toOpenIntent(), forceGrantAction = false, additionalParams = { intent ->
        intent.prepareIntent(bookDesc)
    }) {


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book")
        fun testData(): Iterable<BookFile> {
            return listOf(BookFile("1.10.pdf") , BookFile("2.20.pdf"), BookFile("3.30.pdf"))
        }
    }

    @Test
    fun openViaTemporaryFile() {
        //TODO investigate problem on LOLLIPOP
        if (Build.VERSION.SDK_INT == LOLLIPOP) return
        device.wait(Until.findObject(By.textContains("temporary")), LONG_TIMEOUT)?.click() ?: failWithScreenShot("No dialog 2")
        device.wait(Until.findObject(By.clazz(OrionDrawScene::class.java)), SHORT_TIMEOUT)

        awaitBookLoading()
        onActivity {
            Assert.assertNotNull(it.controller)
            Assert.assertEquals(
                it.controller!!.document.pageCount,
                bookDesc.simpleFileName.substringAfter('.').substringBefore('.').toInt()
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = LOLLIPOP)
    fun openViaNewFile() {
        //screenshotRule.takeScreenshot(bookDesc.simpleFileName)
        device.wait(Until.findObject(By.textContains("new file")), LONG_TIMEOUT)?.click() ?: failWithScreenShot("No dialog")
        screenshotRule.takeScreenshot("new " + bookDesc.simpleFileName )
//        device.wait(Until.findObject(By.clazz(OrionDrawScene::class.java)), SHORT_TIMEOUT)
//
//        awaitBookLoading()
//        onActivity {
//            Assert.assertNotNull(it.controller)
//            Assert.assertEquals(
//                it.controller!!.document.pageCount,
//                bookDesc.simpleFileName.substringAfter('.').substringBefore('.').toInt()
//            )
//        }
    }
}

private fun Intent.prepareIntent(bookDesc: BookFile) {
    val fileName = bookDesc.simpleFileName
    val uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
        .authority(BuildConfig.APPLICATION_ID + ".fileprovider")
        .encodedPath(fileName).appendQueryParameter("displayName", fileName).build()

    instrumentationContext.grantUriPermission(
        universe.constellation.orion.viewer.BuildConfig.APPLICATION_ID,
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    )
    data = uri
}
