package universe.constellation.orion.viewer.prefs

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
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R

class ActionListActivity : OrionBaseActivity() {

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
        val actions = Action.entries.filter { it.isVisible }
        view.adapter = object : ArrayAdapter<Action?>(
            this,
            android.R.layout.simple_list_item_single_choice,
            actions
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as CheckedTextView
                view.setText(actions[position].nameRes)
                return view
            }
        }

        view.choiceMode = ListView.CHOICE_MODE_SINGLE

        val code = intent.getIntExtra("code", 0)

        for (i in actions.indices) {
            val action = actions[i]
            if (action.code == code) {
                view.setItemChecked(i, true)
                //view.setSelection(i);
                break
            }
        }
        OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val code1 = actions[position].code
            val result = Intent()
            result.putExtra("code", code1)
            result.putExtra("keyCode", keyCode)
            result.putExtra("isLong", isLong)
            setResult(RESULT_OK, result)
            finish()
        }.also { view.onItemClickListener = it }
    }


}
