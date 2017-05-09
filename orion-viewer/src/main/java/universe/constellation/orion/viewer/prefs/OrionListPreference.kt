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
import android.preference.ListPreference
import android.util.AttributeSet
import universe.constellation.orion.viewer.R

class OrionListPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        ListPreference(context, attrs), OrionBookPreference {

    override val orionState = State()

    init {
        attrs?.let { init(it) }
    }

    private fun init(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference)
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference_isBook, false)
        a.recycle()
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        orionState.onSetInitialValue = true
        try {
            super.onSetInitialValue(if (isCurrentBookOption) true else restoreValue, defaultValue)
        } finally {
            orionState.onSetInitialValue = false
        }
    }

    override fun persistString(value: String): Boolean {
        persistValue(value)
        return isCurrentBookOption || super.persistString(value)
    }

    override fun persistInt(value: Int): Boolean {
        persistValue(value.toString())
        return isCurrentBookOption || super.persistInt(value)
    }

    override fun getPersistedInt(defaultReturnValue: Int): Int {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedInt(this, defaultReturnValue)
        } else {
            return super.getPersistedInt(defaultReturnValue)
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String? {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedString(this, defaultReturnValue)
        } else {
            return super.getPersistedString(defaultReturnValue)
        }
    }
}
