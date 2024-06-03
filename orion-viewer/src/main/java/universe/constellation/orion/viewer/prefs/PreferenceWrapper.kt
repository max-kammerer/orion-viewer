package universe.constellation.orion.viewer.prefs

import android.content.SharedPreferences

open class PreferenceWrapper(val prefs: SharedPreferences)  {

    fun getIntFromStringProperty(key: String?, defaultValue: Int): Int {
        val value = prefs.getString(key, null)
        val newIntValue = if (value == null || "" == value) {
            defaultValue
        } else {
            value.toInt()
        }
        return newIntValue
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun getStringProperty(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun getNullableStringProperty(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    fun getBooleanProperty(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }


    fun saveBooleanProperty(key: String, newValue: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(key, newValue)
        editor.apply()
    }

    fun saveStringProperty(key: String, newValue: String) {
        val editor = prefs.edit()
        editor.putString(key, newValue)
        editor.apply()
    }

    fun removePreference(name: String?) {
        prefs.edit().remove(name).apply()
    }

    fun putIntPreference(name: String?, value: Int) {
        prefs.edit().putInt(name, value).apply()
    }

    fun removeAll() {
        prefs.edit().clear().apply()
    }

    val allProperties: Map<String, *>
        get() = prefs.all

}