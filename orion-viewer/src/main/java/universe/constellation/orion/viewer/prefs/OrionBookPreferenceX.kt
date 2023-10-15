/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    THRESHOLD("THRESHOLD", LastPageInfo::threshold.name);
    companion object {
        val key2Operation = BookPreferenceKeyX.values().associateBy { it.prefKey }
    }
}

fun createDataStore(context: Context): PreferenceDataStore {
    return object : PreferenceDataStore() {
        private fun getKeyProcessor(key: String) =
            BookPreferenceKeyX.key2Operation[key] ?: error("Please define key for $key")

        private fun persist(key: String, value: String) {
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
            persist(key, value!!)
        }

        override fun putStringSet(key: String, values: MutableSet<String>?) {
            super.putStringSet(key, values)
        }

        override fun putInt(key: String, value: Int) {
            persist(key, value.toString())
        }

        override fun putLong(key: String, value: Long) {
            super.putLong(key, value)
        }

        override fun putFloat(key: String, value: Float) {
            super.putFloat(key, value)
        }

        override fun putBoolean(key: String, value: Boolean) {
            super.putBoolean(key, value)
        }

        override fun getString(key: String, defValue: String?): String? {
            println("key $key = $defValue")
            return get(key, defValue)
        }

        override fun getStringSet(
            key: String,
            defValues: MutableSet<String>?
        ): MutableSet<String>? {
            return super.getStringSet(key, defValues)
        }

        override fun getInt(key: String, defValue: Int): Int {
            return get(key, defValue)
        }

        override fun getLong(key: String, defValue: Long): Long {
            return super.getLong(key, defValue)
        }

        override fun getFloat(key: String, defValue: Float): Float {
            return super.getFloat(key, defValue)
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return super.getBoolean(key, defValue)
        }
    }
}