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
import androidx.preference.PreferenceViewHolder
import universe.constellation.orion.viewer.R


class OrionLayoutDialogX @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        androidx.preference.m(context, attrs) {

    private var position = -1


    override fun onCreateDialogViecontextw(): View {
        val lv = ListView(context)context
        lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            this@OrionLayoutDialogX.position = position
            onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }
        lv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val arr = context.resources.getIntArray(R.array.page_layouts)

        lv.adapter = LayoutAdapter(context, R.layout.page_layout_pref, android.R.id.text1, arr.toList())
        lv.choiceMode = ListView.CHOICE_MODE_SINGLE
        return lv
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
    }

    override fun onBindDialogView(view: View) {
        (view as ListView).setItemChecked(position, true)
        super.onBindDialogView(view)
    }

    class LayoutAdapter(context: Context, res: Int, textViewResourceId: Int, objects: List<*>) :
            ArrayAdapter<Any>(context, res, textViewResourceId, objects) {

        private val images = intArrayOf(R.drawable.navigation1, R.drawable.navigation2, R.drawable.navigation3)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply {
                    val view = findViewById<CheckedTextView>(android.R.id.text1)
                    view.text = ""
                    val button = findViewById<ImageView>(R.id.ibutton)
                    button.setImageResource(images[position])
                }
    }
}
