package universe.constellation.orion.viewer.prefs

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity

/**
 * Moves the app settings between devices or installations (issue #64): the options with the tap
 * zones and defaults for books, and the key bindings, as JSON with the type of every value.
 *
 * What the app keeps about its use rather than how it is set up stays: recent files, the last
 * folder and search, the one-time flags, the options version; test and debug switches too. The
 * parameters of each book and the bookmarks aren't settings either, bookmarks have an export.
 */
object SettingsTransfer {

    private const val FORMAT = "orion-viewer-settings"
    private const val FORMAT_VERSION = 1

    private val STATE_KEYS = setOf(
        OrionFileManagerActivity.LAST_OPENED_DIRECTORY,
        "LAST_SEARCH_QUERY",
        "OPENED_SURVEY",
        "PERMISSION_DIALOG_SHOWN",
        GlobalOptions.SHOW_TAP_HELP,
        GlobalOptions.VERSION,
        GlobalOptions.DEBUG,
        GlobalOptions.TEST_SCREEN_WIDTH,
        GlobalOptions.TEST_SCREEN_HEIGHT,
        GlobalOptions.OPEN_AS_TEMP_BOOK,
        GlobalOptions.TEST_FLAG,
    )

    private fun isSetting(key: String) = key !in STATE_KEYS && !key.startsWith(GlobalOptions.RECENT_PREFIX)

    fun export(options: SharedPreferences, keyBindings: SharedPreferences): String =
        JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("options", options.toJson())
            .put("keyBindings", keyBindings.toJson())
            .toString(2)

    /**
     * Replaces the settings with those of [json]: the ones it lacks go back to their defaults.
     * Throws [IllegalArgumentException] for anything but an export of this app, changing nothing.
     */
    fun import(json: String, options: SharedPreferences, keyBindings: SharedPreferences) {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Not JSON", e)
        }
        require(root.optString("format") == FORMAT) { "Not a settings file" }
        require(root.optInt("version", Int.MAX_VALUE) <= FORMAT_VERSION) { "Settings file of a newer version" }
        //read both before writing anything: a broken file leaves the settings as they were
        val (newOptions, newKeyBindings) = try {
            root.getJSONObject("options").toValues() to root.getJSONObject("keyBindings").toValues()
        } catch (e: JSONException) {
            throw IllegalArgumentException("Broken settings file", e)
        }
        options.replaceWith(newOptions)
        keyBindings.replaceWith(newKeyBindings)
    }

    private fun SharedPreferences.toJson(): JSONObject {
        val result = JSONObject()
        all.filterKeys(::isSetting).toSortedMap().forEach { (key, value) ->
            val (type, json) = when (value) {
                is Boolean -> "boolean" to value
                is Int -> "int" to value
                is Long -> "long" to value
                is Float -> "float" to value.toDouble()
                is String -> "string" to value
                is Set<*> -> "stringSet" to JSONArray(value.map { it.toString() }.sorted())
                else -> return@forEach
            }
            result.put(key, JSONObject().put("type", type).put("value", json))
        }
        return result
    }

    private fun JSONObject.toValues(): Map<String, Any> =
        keys().asSequence().filter(::isSetting).associateWith { key ->
            val entry = getJSONObject(key)
            when (val type = entry.getString("type")) {
                "boolean" -> entry.getBoolean("value")
                "int" -> entry.getInt("value")
                "long" -> entry.getLong("value")
                "float" -> entry.getDouble("value").toFloat()
                "string" -> entry.getString("value")
                "stringSet" -> entry.getJSONArray("value").let { array -> (0 until array.length()).map { array.getString(it) }.toSet() }
                else -> throw IllegalArgumentException("Unknown type $type of $key")
            }
        }

    /** Committed, not applied: the options' listener updates the open app right away. */
    private fun SharedPreferences.replaceWith(values: Map<String, Any>) {
        val editor = edit()
        all.keys.filter(::isSetting).filter { it !in values }.forEach { editor.remove(it) }
        values.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value as Set<String>)
            }
        }
        editor.commit()
    }
}
