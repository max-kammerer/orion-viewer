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
import android.content.res.TypedArray
import android.text.InputType
import android.util.AttributeSet
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import universe.constellation.orion.viewer.R
import java.util.regex.Pattern

open class OrionEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?
) : EditTextPreference(context, attrs) {

    private var minValue: Int? = null
    private var maxValue: Int? = null

    private var pattern: String? = null

    private var originalSummary: CharSequence? = null

    init {
        attrs?.let { init(it) }
    }

    fun onPreferenceChange(
        newValue: String?
    ): String? {
        if (minValue != null || maxValue != null) {
            if (newValue == null || "" == newValue) {
                return "Value couldn't be empty!"
            }

            val value = newValue.toIntOrNull() ?: return "Invalid number: $newValue"

            if (minValue != null && minValue!! > value) {
                return "New value should be greater or equal than " + minValue!!
            }

            if (maxValue != null && maxValue!! < value) {
                return "New value should be less or equal than " + maxValue!!
            }
        }

        if (pattern != null && !Pattern.compile(pattern!!).matcher(newValue!!)
                .matches()
        ) {
            return "Couldn't set value: wrong interval!"
        }

        return null
    }


    private fun init(attrs: AttributeSet) {
        originalSummary = summary
        context.obtainStyledAttributes(
            attrs,
            R.styleable.universe_constellation_orion_viewer_prefs_OrionEditTextPreference
        ).let {
            pattern =
                it.getString(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditTextPreference_pattern)
            minValue = getIntegerOrNull(
                it,
                R.styleable.universe_constellation_orion_viewer_prefs_OrionEditTextPreference_minValue
            )
            maxValue = getIntegerOrNull(
                it,
                R.styleable.universe_constellation_orion_viewer_prefs_OrionEditTextPreference_maxValue
            )
            it.close()
        }

        if (pattern != null || minValue != null || maxValue != null) {
            setOnBindEditTextListener { editText ->
                if (minValue != null || maxValue != null) {
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                }
                editText.doAfterTextChanged { editable ->
                    onPreferenceChange(editable?.toString() ?: return@doAfterTextChanged)?.let {
                        editText.error = it
                    }
                }
            }
        }
    }

    private fun getIntegerOrNull(array: TypedArray, id: Int): Int? {
        val undefined = Int.MIN_VALUE
        val value = array.getInt(id, undefined)
        return if (value == undefined) {
            null
        } else {
            value
        }
    }

}