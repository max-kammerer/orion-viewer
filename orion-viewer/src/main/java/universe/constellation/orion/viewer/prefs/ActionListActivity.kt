package universe.constellation.orion.viewer.prefs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import android.widget.TextView
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.ActionGroup
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R

class ActionListActivity : OrionBaseActivity() {

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.actions_selection, false)

        val type = intent.getIntExtra("type", 0)
        val header = findViewById<TextView>(R.id.actions_header)
        header.setText(if (type == 0) R.string.short_click else if (type == 1) R.string.long_click else R.string.binding_click)
        val keyCode = intent.getIntExtra("keyCode", 0)
        val isLong = intent.getBooleanExtra("isLong", false)
        if (type == 2) {
            val name = KeyEventNamer.getKeyName(keyCode)
            header.text =
                header.text.toString() + " " + name + (if (isLong) " [long press]" else "")
        }

        val view = findViewById<ListView>(R.id.actionsGroup)
        //"None" first on its own, then every group under its header
        val rows = ArrayList<Any>()
        rows.add(Action.NONE)
        for (group in ActionGroup.entries) {
            rows.add(group)
            Action.entries.filterTo(rows) { it.group == group }
        }
        view.adapter = object : ArrayAdapter<Any>(this, android.R.layout.simple_list_item_single_choice, rows) {
            override fun getViewTypeCount() = 2

            override fun getItemViewType(position: Int) = if (rows[position] is ActionGroup) 1 else 0

            override fun areAllItemsEnabled() = false

            override fun isEnabled(position: Int) = rows[position] is Action

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return when (val row = rows[position]) {
                    is ActionGroup -> {
                        val header = (convertView as? TextView)
                            ?: layoutInflater.inflate(android.R.layout.preference_category, parent, false) as TextView
                        header.setText(row.titleRes)
                        header
                    }
                    else -> {
                        val item = (convertView as? CheckedTextView)
                            ?: layoutInflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false) as CheckedTextView
                        item.setText((row as Action).nameRes)
                        item
                    }
                }
            }
        }

        view.choiceMode = ListView.CHOICE_MODE_SINGLE

        val code = intent.getIntExtra("code", 0)

        for (i in rows.indices) {
            val action = rows[i] as? Action ?: continue
            if (action.code == code) {
                view.setItemChecked(i, true)
                break
            }
        }
        OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val code1 = (rows[position] as Action).code
            val result = Intent()
            result.putExtra("code", code1)
            result.putExtra("keyCode", keyCode)
            result.putExtra("isLong", isLong)
            setResult(RESULT_OK, result)
            finish()
        }.also { view.onItemClickListener = it }
    }


}
