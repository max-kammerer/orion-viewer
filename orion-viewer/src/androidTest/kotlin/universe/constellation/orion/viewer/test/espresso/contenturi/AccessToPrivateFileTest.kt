package universe.constellation.orion.viewer.test.espresso.contenturi

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import android.widget.Button
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.hamcrest.core.AllOf
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.cacheContentFolder
import universe.constellation.orion.viewer.test.framework.BaseTestWithActivity
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.SHORT_TIMEOUT
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.createContentIntentWithGenerated
import universe.constellation.orion.viewer.test.framework.doFail
import universe.constellation.orion.viewer.test.framework.onActivity

fun onTextNotButtonView(stringId: Int): ViewInteraction {
    return onView(
        AllOf.allOf(
            withText(stringId),
            IsNot.not(ViewMatchers.isAssignableFrom(Button::class.java))
        ))
}

@SdkSuppress(minSdkVersion = KITKAT)
@RunWith(Parameterized::class)
class AccessToPrivateFileTest(private val simpleFileName: String) :
    BaseTestWithActivity(createContentIntentWithGenerated(simpleFileName)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Test for {0} book")
        fun testData(): Iterable<String> {
            return listOf("1.10.pdf", "2.20.pdf")
        }
    }

    @After
    fun clean() {
        appContext.cacheContentFolder().deleteRecursively()
    }

    @Test
    fun openViaTemporaryFile() {
        onTextNotButtonView(R.string.fileopen_open_in_temporary_file).perform(ViewActions.click())
        checkFileWasOpened()
    }

    @Test
    fun openViaNewFile() {
        //TODO investigate problem on LOLLIPOP and KITKAT with test framework
        if (Build.VERSION.SDK_INT <= N) return

        onTextNotButtonView(R.string.fileopen_save_to_file).perform(ViewActions.click())
        processEmulatorErrors()

        if (Build.VERSION.SDK_INT <= M && device.wait(Until.findObject(By.textContains("Save to")), LONG_TIMEOUT) != null) {
            device.wait(Until.findObject(By.textContains("Downloads")), SHORT_TIMEOUT)?.click()
        }

        val editField = device.wait(Until.findObject(By.clazz(EditText::class.java)), LONG_TIMEOUT) ?: doFail("No edit field")
        editField.text = simpleFileName

        val saveButton =
            device.findObject(By.textContains("SAVE"))
                ?: device.findObject(By.clazz(Button::class.java))
                ?: doFail("No save button")

        saveButton.click()

        checkFileWasOpened()
    }

    private fun checkFileWasOpened() {
        onView(ViewMatchers.withId(R.id.view)).check(matches(isCompletelyDisplayed()))
        onActivity {
            Assert.assertNotNull(it.controller)
            Assert.assertEquals(
                it.controller!!.document.pageCount,
                simpleFileName.substringAfter('.').substringBefore('.').toInt()
            )
        }
    }
}