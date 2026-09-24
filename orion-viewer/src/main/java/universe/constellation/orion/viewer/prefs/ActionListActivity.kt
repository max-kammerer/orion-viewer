package universe.constellation.orion.viewer.prefs

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.doAfterTextChanged
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.ActionGroup
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R

/**
 * Picks the action of a tap zone or a key: "None" first, then every action under the header of
 * its group. A header tap folds the group; a query in the search box shows the matching actions
 * of every group instead.
 */
class ActionListActivity : OrionBaseActivity() {

    private val collapsed = HashSet<ActionGroup>()

    private var query = ""

    private var selected: Action? = null

    private val rows = ArrayList<Any>()

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

        val code = intent.getIntExtra("code", 0)
        selected = Action.entries.firstOrNull { it.code == code }
        //ten rows for hardware keys of e-ink readers: folded unless one of them is the choice
        if (selected?.group != ActionGroup.CROP_KEYS) collapsed.add(ActionGroup.CROP_KEYS)

        val view = findViewById<ListView>(R.id.actionsGroup)
        val adapter = object : ArrayAdapter<Any>(this, android.R.layout.simple_list_item_single_choice, rows) {
            override fun getViewTypeCount() = 2

            override fun getItemViewType(position: Int) = if (rows[position] is ActionGroup) 1 else 0

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return when (val row = rows[position]) {
                    is ActionGroup -> {
                        val header = (convertView as? TextView)
                            ?: (layoutInflater.inflate(android.R.layout.preference_category, parent, false) as TextView).apply {
                                compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.action_group_icon_padding)
                            }
                        header.setText(row.titleRes)
                        //the group icon at the start, a chevron at the end while the group is folded
                        val folded = row in collapsed && query.isEmpty()
                        header.setCompoundDrawablesRelative(
                            headerIcon(row.iconRes, header),
                            null,
                            if (folded) headerIcon(R.drawable.outline_chevron_right, header) else null,
                            null
                        )
                        header
                    }
                    else -> {
                        val item = (convertView as? CheckedTextView)
                            ?: layoutInflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false) as CheckedTextView
                        item.setText((row as Action).nameRes)
                        item.isChecked = row == selected
                        item
                    }
                }
            }
        }
        view.adapter = adapter
        rebuildRows()

        view.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            when (val row = rows[position]) {
                is ActionGroup -> {
                    if (query.isEmpty()) {
                        if (!collapsed.remove(row)) collapsed.add(row)
                        rebuildRows()
                    }
                }
                is Action -> {
                    val result = Intent()
                    result.putExtra("code", row.code)
                    result.putExtra("keyCode", keyCode)
                    result.putExtra("isLong", isLong)
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        }

        findViewById<EditText>(R.id.actions_search).doAfterTextChanged {
            query = it?.toString()?.trim() ?: ""
            rebuildRows()
        }
    }

    /** A header icon at the size and in the colour of the header text. */
    private fun headerIcon(@DrawableRes res: Int, header: TextView): Drawable? {
        val drawable = AppCompatResources.getDrawable(this, res) ?: return null
        val size = resources.getDimensionPixelSize(R.dimen.action_group_icon_size)
        return DrawableCompat.wrap(drawable.mutate()).apply {
            setBounds(0, 0, size, size)
            DrawableCompat.setTintList(this, header.textColors)
        }
    }

    /** The list for the current query and folding; the adapter shares [rows]. */
    private fun rebuildRows() {
        rows.clear()
        if (matches(Action.NONE)) rows.add(Action.NONE)
        for (group in ActionGroup.entries) {
            val actions = Action.entries.filter { it.group == group && matches(it) }
            if (actions.isEmpty()) continue
            rows.add(group)
            if (query.isNotEmpty() || group !in collapsed) rows.addAll(actions)
        }
        (findViewById<ListView>(R.id.actionsGroup).adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun matches(action: Action): Boolean =
        query.isEmpty() || getString(action.nameRes).contains(query, ignoreCase = true)
}
