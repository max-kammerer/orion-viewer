/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.log

class OrionTapActivity : OrionBaseActivity(false) {

    private var activeView: View? = null
    private var index: Int = 0
    private var isLong: Boolean = false
    private val myCode = Array(9) { IntArray(2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tap)
        val table = findViewById<View>(R.id.tap_table) as TableLayout
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val layout = row.getChildAt(j)
                layout.isClickable = true
                layout.isLongClickable = true
                val shortText = layout.findViewById<View>(R.id.shortClick) as TextView
                val longText = layout.findViewById<View>(R.id.longClick) as TextView

                var shortCode = prefs.getInt(getKey(i, j, false), -1)
                var longCode = prefs.getInt(getKey(i, j, true), -1)
                if (shortCode == -1) {
                    shortCode = getDefaultAction(i, j, false)
                }
                if (longCode == -1) {
                    longCode = getDefaultAction(i, j, true)
                }
                val shortAction = Action.getAction(shortCode)
                val longAction = Action.getAction(longCode)
                shortText.text = resources.getString(shortAction.getName())
                longText.text = resources.getString(longAction.getName())
                val index = i * 3 + j
                myCode[index][0] = shortCode
                myCode[index][1] = longCode
                layout.setOnClickListener { v -> selectAction(v, false, index) }

                layout.setOnLongClickListener { v -> selectAction(v, true, index) }
            }
        }
    }

    private fun selectAction(view: View, isLong: Boolean, index: Int): Boolean {
        val intent = Intent(this@OrionTapActivity, ActionListActivity::class.java)
        intent.putExtra("code", myCode[index][if (isLong) 1 else 0])
        intent.putExtra("type", if (isLong) 1 else 0)
        activeView = view
        this.isLong = isLong
        this.index = index
        startActivityForResult(intent, 1)
        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (activeView != null) {
                val view = activeView!!.findViewById<View>(if (isLong) R.id.longClick else R.id.shortClick) as TextView
                val code = data!!.getIntExtra("code", 0)
                val action = Action.getAction(code)
                myCode[index][if (isLong) 1 else 0] = action.code
                view.text = resources.getString(action.getName())

                val i = index / 3
                val j = index % 3
                log(index.toString() + " " + i + " " + j)
                val pref = PreferenceManager.getDefaultSharedPreferences(this)
                val ed = pref.edit()
                ed.putInt(getKey(i, j, isLong), action.code)
                ed.commit()
            }
        }
    }


    companion object {

        @JvmStatic
        fun getDefaultAction(row: Int, column: Int, isLong: Boolean): Int {
            return if (row == 1 && column == 1) {
                if (isLong) Action.OPTIONS.code else Action.MENU.code
            } else {
                if (2 - row < column) {
                    if (isLong) Action.NEXT.code else Action.NEXT.code
                } else {
                    if (isLong) Action.PREV.code else Action.PREV.code
                }
            }
        }

        @JvmStatic
        fun getKey(i: Int, j: Int, isLong: Boolean): String {
            return GlobalOptions.TAP_ZONE + (if (isLong) "_LONG_CLICK_" else "_SHORT_CLICK_") + i + "_" + j
        }
    }
}
