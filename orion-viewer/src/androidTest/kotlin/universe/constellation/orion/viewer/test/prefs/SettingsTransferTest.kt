package universe.constellation.orion.viewer.test.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import universe.constellation.orion.viewer.prefs.SettingsTransfer

/** Settings go to a file and back whole, types included; what the app keeps about its use doesn't. */
class SettingsTransferTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun prefs(name: String): SharedPreferences =
        context.getSharedPreferences("settings_transfer_test_$name", Context.MODE_PRIVATE).also { it.edit().clear().commit() }

    private val options = prefs("options")
    private val keys = prefs("keys")
    private val otherOptions = prefs("other_options")
    private val otherKeys = prefs("other_keys")

    @After
    fun cleanUp() {
        listOf(options, keys, otherOptions, otherKeys).forEach { it.edit().clear().commit() }
    }

    @Test
    fun settingsMoveWithTheirTypes() {
        options.edit()
            .putString("WALK_ORDER", "ACBD").putBoolean("FULL_SCREEN", true).putInt("DEFAULT_CROP_LEFT", 7)
            .putLong("SOME_LONG", 1L shl 40).putFloat("SOME_FLOAT", 1.5f).putStringSet("SOME_SET", setOf("a", "b"))
            .putInt("TAP_ZONE_0_0", 12)
            //what the app keeps about its use stays on the device
            .putString("recent_0", "/sdcard/book.pdf").putBoolean("PERMISSION_DIALOG_SHOWN", true).putString("LAST_OPENED_DIR", "/sdcard")
            .commit()
        keys.edit().putInt("24", 5).commit()

        val json = SettingsTransfer.export(options, keys)
        assertFalse(json, json.contains("recent_0") || json.contains("PERMISSION_DIALOG_SHOWN") || json.contains("LAST_OPENED_DIR"))

        otherOptions.edit().putString("WALK_ORDER", "BADC").putBoolean("OLD_ONLY_SETTING", true).putString("recent_0", "/sdcard/mine.pdf").commit()
        SettingsTransfer.import(json, otherOptions, otherKeys)

        assertEquals("ACBD", otherOptions.getString("WALK_ORDER", null))
        assertTrue(otherOptions.getBoolean("FULL_SCREEN", false))
        assertEquals(7, otherOptions.getInt("DEFAULT_CROP_LEFT", 0))
        assertEquals(1L shl 40, otherOptions.getLong("SOME_LONG", 0))
        assertEquals(1.5f, otherOptions.getFloat("SOME_FLOAT", 0f), 0f)
        assertEquals(setOf("a", "b"), otherOptions.getStringSet("SOME_SET", null))
        assertEquals(12, otherOptions.getInt("TAP_ZONE_0_0", 0))
        assertEquals(5, otherKeys.getInt("24", 0))
        //the file replaces the settings: one it lacks goes back to its default
        assertFalse(otherOptions.contains("OLD_ONLY_SETTING"))
        //the device's own state is kept
        assertEquals("/sdcard/mine.pdf", otherOptions.getString("recent_0", null))
    }

    @Test
    fun otherFilesChangeNothing() {
        otherOptions.edit().putString("WALK_ORDER", "BADC").commit()
        listOf(
            "not json",
            """{"format":"something-else","options":{},"keyBindings":{}}""",
            """{"format":"orion-viewer-settings","version":1,"options":{"WALK_ORDER":{"type":"string"}},"keyBindings":{}}""",
            """{"format":"orion-viewer-settings","version":99,"options":{},"keyBindings":{}}""",
        ).forEach { json ->
            try {
                SettingsTransfer.import(json, otherOptions, otherKeys)
                fail("Imported: $json")
            } catch (e: IllegalArgumentException) {
                assertEquals(json, "BADC", otherOptions.getString("WALK_ORDER", null))
            }
        }
    }
}
