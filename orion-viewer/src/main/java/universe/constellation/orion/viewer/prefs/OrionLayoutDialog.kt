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
import android.content.DialogInterface
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import universe.constellation.orion.viewer.R


class OrionLayoutDialog @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        DialogPreference(context, attrs), OrionBookPreference {

    override val orionState = State()

    private var position = -1

    init {
        positiveButtonText = null
        attrs?.let { init(it) }
    }


    private fun init(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionLayoutDialog)
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionLayoutDialog_isBook, false)
        a.recycle()
    }

    override fun onCreateDialogView(): View {
        val lv = ListView(context)
        lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            this@OrionLayoutDialog.position = position
            onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }
        lv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val arr = context.resources.getIntArray(R.array.page_layouts)

        lv.adapter = LayoutAdapter(context, R.layout.page_layout_pref, android.R.id.text1, arr.toList())
        lv.choiceMode = ListView.CHOICE_MODE_SINGLE
        return lv
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, 0)
    }

    override fun onBindDialogView(view: View) {
        (view as ListView).setItemChecked(position, true)
        super.onBindDialogView(view)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && position >= 0) {
            if (callChangeListener(position)) {
                persistInt(position)
            }
        }
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        orionState.onSetInitialValue = true
        try {
            position = if (isCurrentBookOption || restoreValue) {
                getPersistedInt(0)
            } else {
                defaultValue as Int
            }
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
        return if (isCurrentBookOption) {
            OrionPreferenceUtil.getPersistedInt(this, defaultReturnValue)
        } else {
            super.getPersistedInt(defaultReturnValue)
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String? {
        return if (isCurrentBookOption) {
            OrionPreferenceUtil.getPersistedString(this, defaultReturnValue)
        } else {
            super.getPersistedString(defaultReturnValue)
        }
    }

    class LayoutAdapter(context: Context, res: Int, textViewResourceId: Int, objects: List<*>) :
            ArrayAdapter<Any>(context, res, textViewResourceId, objects) {

        private val images = intArrayOf(R.drawable.navigation1, R.drawable.navigation2, R.drawable.navigation3)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply {
                    val view = findViewById(android.R.id.text1) as CheckedTextView
                    view.text = ""
                    val button = findViewById(R.id.ibutton) as ImageView
                    button.setImageResource(images[position])
                }
    }
}
