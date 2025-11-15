package universe.constellation.orion.viewer.prefs

import androidx.lifecycle.LiveData

class Preference<T>(val key: String, val defaultValue: T, val extractor: Preference<T>.() -> T): LiveData<T>() {

    init {
        setValue(extractor())
    }

    override fun getValue(): T {
        return super.getValue() as T
    }

    fun update() {
        setValue(extractor())
    }
}

internal inline fun <reified T> GlobalOptions.pref(key: String, defaultValue: T, stringAsInt: Boolean = false): Preference<T> {
    val extractor: Preference<T>.() -> T = when (T::class) {
        String::class -> {
            { this@pref.prefs.getString(this.key, this.defaultValue as String) as T }
        }
        Boolean::class -> {
            { this@pref.prefs.getBoolean(this.key, this.defaultValue as Boolean) as T }
        }

        Int::class -> {
            if (!stringAsInt) {
                { this@pref.prefs.getInt(this.key, this.defaultValue as Int) as T }
            } else {
                { this@pref.getIntFromStringProperty(this.key, this.defaultValue as Int) as T }
            }
        }

        else -> error("Unsupported type ${T::class.java}")
    }

    return Preference(key, defaultValue, extractor).also { subscribe(it) }
}