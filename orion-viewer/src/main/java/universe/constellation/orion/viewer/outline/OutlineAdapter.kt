/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2026  Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.outline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.document.OutlineItem

/**
 * Outline tree as a flat RecyclerView list of the currently visible nodes.
 *
 * The tree structure is derived from [OutlineItem.level] of the document-ordered [items]:
 * a node's children are the following items with a greater level, up to the next item with
 * the same or smaller level.
 */
class OutlineAdapter(
    private val items: Array<OutlineItem>,
    currentPage: Int,
    private val onNavigate: (OutlineItem) -> Unit
) : RecyclerView.Adapter<OutlineAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val expander: AppCompatImageView = view.findViewById(R.id.expander)
        val title: TextView = view.findViewById(R.id.title)
        val page: TextView = view.findViewById(R.id.page)
    }

    private val hasChildren = BooleanArray(items.size)

    /** Index of the first item after i not belonging to i's subtree. */
    private val subtreeEnd = IntArray(items.size)

    private val expanded = BooleanArray(items.size)

    /** Item indices currently visible, in document order. */
    private val visible = ArrayList<Int>()

    /** Item highlighted as the current position, -1 if none. */
    private val currentIndex: Int

    /** Adapter position to scroll to initially, -1 if none. */
    val initialPosition: Int

    init {
        for (i in items.indices) {
            var j = i + 1
            while (j < items.size && items[j].level > items[i].level) j++
            subtreeEnd[i] = j
            hasChildren[i] = j > i + 1
        }

        currentIndex = items.indexOfLast { it.page in 0..currentPage }
        if (currentIndex != -1) {
            expanded[currentIndex] = true
            var parent = parentOf(currentIndex)
            while (parent != -1) {
                expanded[parent] = true
                parent = parentOf(parent)
            }
        }
        rebuildVisible()
        initialPosition = visible.indexOf(currentIndex)
        setHasStableIds(true)
    }

    private fun parentOf(index: Int): Int {
        for (i in index - 1 downTo 0) {
            if (items[i].level < items[index].level) return i
        }
        return -1
    }

    private fun rebuildVisible() {
        visible.clear()
        var i = 0
        while (i < items.size) {
            visible.add(i)
            i = if (hasChildren[i] && !expanded[i]) subtreeEnd[i] else i + 1
        }
    }

    /** Visible part of [index]'s subtree, honoring nested collapsed nodes. */
    private fun visibleSubtree(index: Int): List<Int> {
        val result = ArrayList<Int>()
        var i = index + 1
        while (i < subtreeEnd[index]) {
            result.add(i)
            i = if (hasChildren[i] && !expanded[i]) subtreeEnd[i] else i + 1
        }
        return result
    }

    fun toggle(position: Int) {
        val index = visible[position]
        if (!hasChildren[index]) return
        if (expanded[index]) {
            expanded[index] = false
            var count = 0
            while (position + 1 + count < visible.size && visible[position + 1 + count] < subtreeEnd[index]) count++
            repeat(count) { visible.removeAt(position + 1) }
            notifyItemRangeRemoved(position + 1, count)
        } else {
            expanded[index] = true
            val subtree = visibleSubtree(index)
            visible.addAll(position + 1, subtree)
            notifyItemRangeInserted(position + 1, subtree.size)
        }
        notifyItemChanged(position)
    }

    fun expandAll() {
        for (i in items.indices) expanded[i] = hasChildren[i]
        rebuildVisible()
        notifyDataSetChanged()
    }

    fun collapseAll() {
        expanded.fill(false)
        rebuildVisible()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = visible.size

    override fun getItemId(position: Int): Long = visible[position].toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.outline_entry, parent, false)
        val holder = Holder(view)
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val item = items[visible[position]]
            if (item.page >= 0) onNavigate(item) else toggle(position)
        }
        holder.expander.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) toggle(position)
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val index = visible[position]
        val item = items[index]

        val density = holder.itemView.resources.displayMetrics.density
        holder.itemView.setPadding(
            (INDENT_PER_LEVEL_DP * item.level * density).toInt(), 0,
            holder.itemView.paddingRight, 0
        )

        if (hasChildren[index]) {
            holder.expander.visibility = View.VISIBLE
            holder.expander.setImageResource(
                if (expanded[index]) R.drawable.outline_chevron_down else R.drawable.outline_chevron_right
            )
        } else {
            holder.expander.visibility = View.INVISIBLE
        }

        holder.title.text = item.title
        holder.page.text = if (item.page < 0) " " else (item.page + 1).toString()

        val isCurrent = index == currentIndex
        holder.itemView.setBackgroundColor(if (isCurrent) holder.itemView.context.listHighlightColor else 0)
        holder.title.setTypeface(null, if (isCurrent) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    companion object {
        private const val INDENT_PER_LEVEL_DP = 20
    }
}

/* Falls back to orion_blue at 30% alpha for themes that don't define the attribute. */
internal val android.content.Context.listHighlightColor: Int
    get() {
        return  0x4D54759E
    }
