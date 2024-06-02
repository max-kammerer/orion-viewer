package universe.constellation.orion.viewer.prefs

class Preference<T>(val key: String, val defaultValue: T, val extractor: Preference<T>.() -> T) {

    var value: T = extractor()
        private set

    fun update() {
        value = extractor()
    }
}

internal inline fun <reified T> GlobalOptions.pref(key: String, defaultValue: T): Preference<T> {
    val extractor: Preference<T>.() -> T = when (T::class) {
        String::class -> {
            { this@pref.prefs.getString(this.key, this.defaultValue as String) as T }
        }
        Boolean::class -> {
            { this@pref.prefs.getBoolean(this.key, this.defaultValue as Boolean) as T }
        }

        Int::class -> {
            { this@pref.prefs.getInt(this.key, this.defaultValue as Int) as T }
        }

        else -> error("Unsupported type ${T::class.java}")
    }

    return Preference(key, defaultValue, extractor)
}