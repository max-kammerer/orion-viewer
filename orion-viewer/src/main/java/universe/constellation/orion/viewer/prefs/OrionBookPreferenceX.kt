package universe.constellation.orion.viewer.prefs

import android.content.Context
import androidx.preference.PreferenceDataStore
import universe.constellation.orion.viewer.LastPageInfo

enum class BookPreferenceKeyX(
        val prefKey: String,
        val bookKey: String
) {
    ZOOM(GlobalOptions.DEFAULT_ZOOM, LastPageInfo::zoom.name),
    PAGE_LAYOUT(GlobalOptions.PAGE_LAYOUT, LastPageInfo::pageLayout.name),
    WALK_ORDER(GlobalOptions.WALK_ORDER, LastPageInfo::walkOrder.name),
    SCREEN_ORIENTATION(GlobalOptions.SCREEN_ORIENTATION, LastPageInfo::screenOrientation.name),
    COLOR_MODE(GlobalOptions.COLOR_MODE, LastPageInfo::colorMode.name),
    CONTRAST(GlobalOptions.DEFAULT_CONTRAST, LastPageInfo::contrast.name),
    THRESHOLD("THRESHOLD", LastPageInfo::threshold.name),
    DICTIONARY(GlobalOptions.DICTIONARY, LastPageInfo::dictionary.name);

    companion object {
        val key2Operation = entries.associateBy { it.prefKey }
    }
}

fun createDataStore(context: Context): PreferenceDataStore {
    return object : PreferenceDataStore() {
        private fun getKeyProcessor(key: String) =
            BookPreferenceKeyX.key2Operation[key] ?: error("Please define key for $key")

        private fun persist(key: String, value: String?) {
            val prefKey = getKeyProcessor(key)
            OrionPreferenceUtil.persistValue(
                context.applicationContext as OrionApplication, prefKey.bookKey, value
            )
        }

        private fun get(key: String, defaultValue: String?): String? {
            val prefKey = getKeyProcessor(key)
            return OrionPreferenceUtil.getPersistedString(
                prefKey.bookKey, defaultValue, context.applicationContext as OrionApplication
            )
        }

        private fun get(key: String, defaultValue: Int): Int {
            val prefKey = getKeyProcessor(key)
            return OrionPreferenceUtil.getPersistedInt(
                prefKey.bookKey, defaultValue, context.applicationContext as OrionApplication
            )
        }

        override fun putString(key: String, value: String?) {
            persist(key, value)
        }

        override fun putInt(key: String, value: Int) {
            persist(key, value.toString())
        }

        override fun getString(key: String, defValue: String?): String? {
            return get(key, defValue)
        }


        override fun getInt(key: String, defValue: Int): Int {
            return get(key, defValue)
        }
    }
}