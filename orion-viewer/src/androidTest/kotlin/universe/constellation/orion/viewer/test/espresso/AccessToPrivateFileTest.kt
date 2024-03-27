package universe.constellation.orion.viewer.test.espresso

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.filemanager.fileExtension
import universe.constellation.orion.viewer.test.BuildConfig
import universe.constellation.orion.viewer.test.framework.BookFile
import universe.constellation.orion.viewer.test.framework.BaseUITest
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.SHORT_TIMEOUT
import universe.constellation.orion.viewer.test.framework.failWithScreenShot
import universe.constellation.orion.viewer.test.framework.instrumentationContext
import universe.constellation.orion.viewer.test.framework.onActivity
import universe.constellation.orion.viewer.view.OrionDrawScene


@SdkSuppress(minSdkVersion = KITKAT)
@RunWith(Parameterized::class)
class AccessToPrivateFileTest(val bookDesc: BookFile) :
    BaseUITest(bookDesc.toOpenIntent(), doGrantAction = false, additionalParams = { intent ->
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
        //TODO investigate problem on LOLLIPOP with test framework
        if (Build.VERSION.SDK_INT == LOLLIPOP) return
        device.wait(Until.findObject(By.textContains("temporary")), LONG_TIMEOUT)?.click() ?: failWithScreenShot("No dialog")

        checkFileWasOpen()
    }

    @Test
    fun openViaNewFile() {
        //TODO investigate problem on LOLLIPOP and KITKAT with test framework
        if (Build.VERSION.SDK_INT <= LOLLIPOP) return

        device.wait(Until.findObject(By.textContains("new file")), LONG_TIMEOUT)?.click() ?: failWithScreenShot("No dialog")
        processEmulatorErrors()

        if (Build.VERSION.SDK_INT <= M && device.wait(Until.findObject(By.textContains("Save to")), LONG_TIMEOUT) != null) {
            device.wait(Until.findObject(By.textContains("Downloads")), SHORT_TIMEOUT)?.click()
        }

        val editField = device.wait(Until.findObject(By.clazz(EditText::class.java)), LONG_TIMEOUT) ?: failWithScreenShot("No edit field")
        editField.text = bookDesc.simpleFileName

        val saveButton =
            device.findObject(By.textContains("SAVE"))
                ?: device.findObject(By.clazz(Button::class.java))
                ?: failWithScreenShot("No save button")

        saveButton.click()

        checkFileWasOpen()
    }

    private fun checkFileWasOpen() {
        device.wait(Until.findObject(By.clazz(OrionDrawScene::class.java)), LONG_TIMEOUT)
        awaitBookLoading()
        onActivity {
            Assert.assertNotNull(it.controller)
            Assert.assertEquals(
                it.controller!!.document.pageCount,
                bookDesc.simpleFileName.substringAfter('.').substringBefore('.').toInt()
            )
        }
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
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.fileExtension)
    setDataAndType(uri, mimeType)
}
