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

package universe.constellation.orion.viewer.outline

import android.app.Dialog
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import pl.polidea.treeview.AbstractTreeViewAdapter
import pl.polidea.treeview.InMemoryTreeStateManager
import pl.polidea.treeview.TreeBuilder
import pl.polidea.treeview.TreeNodeInfo
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.log

class OutlineAdapter(
    private val controller: Controller,
    activity: OrionViewerActivity,
    private val dialog: Dialog,
    manager: InMemoryTreeStateManager<Int>,
    private val items: Array<OutlineItem>
) : AbstractTreeViewAdapter<Int>(activity, manager, 20) {


    override fun getNewChildView(treeNodeInfo: TreeNodeInfo<Int>): View {
        val viewLayout = activity
            .layoutInflater.inflate(R.layout.outline_entry, null) as LinearLayout
        return updateView(viewLayout, treeNodeInfo)
    }

    private fun getDescription(id: Int): String {
        return this.items[id].title
    }

    private fun getPage(id: Int): Int {
        return this.items[id].page
    }

    override fun updateView(
        view: View,
        treeNodeInfo: TreeNodeInfo<Int>
    ): LinearLayout {
        val viewLayout = view as LinearLayout

        view.findViewById<TextView>(R.id.title).text = getDescription(treeNodeInfo.id)
        val page0 = getPage(treeNodeInfo.id)
        val textView = view.findViewById<TextView>(R.id.page)
        if (page0 < 0) {
            textView.text = " "
        } else {
            textView.text = (page0 + 1).toString()
        }

        return viewLayout
    }

    override fun getItemId(position: Int): Long {
        return getTreeId(position).toLong()
    }

    override fun getItem(position: Int): Any {
        val id = getItemId(position).toInt()
        return this.items[id]
    }

    override fun handleItemClick(view: View, id: Any) {
        val longId = id as Int
        val info = manager.getNodeInfo(longId)
        val outlineItem = this.items[longId]
        if (outlineItem.page < 0) return

        if (false && info.isWithChildren) {
            super.handleItemClick(view, id)
        } else {
            try {
                controller.drawPage(outlineItem.page, isTapNavigation = true)
                this.dialog.dismiss()
            } catch (e: Exception) {
                log(e)
                val viewerActivity = activity as OrionViewerActivity
                viewerActivity.analytics.error(e, outlineItem.toString())
                viewerActivity.showWarning(activity.getString(R.string.wrong_outline_item, e.message))
            }
        }
    }

    companion object {

        @JvmStatic
        fun initializeTreeManager(manager: InMemoryTreeStateManager<Int>, items: Array<OutlineItem>, currentPage: Int): Int {
            val builder = TreeBuilder(manager)

            var openAtIndex = -1
            var lastItem: OutlineItem? = null
            log("OutlineAdapter:: initializeTreeManager $currentPage")
            for (i in items.indices) {
                val curItem = items[i]
                val last = i - 1
                if (lastItem != null && curItem.level > lastItem.level) {
                    builder.addRelation(last, i)
                } else {
                    builder.sequentiallyAddNextNode(i, curItem.level)
                }

                if (curItem.page <= currentPage) {
                    openAtIndex = i
                }
                lastItem = curItem
            }

            var resultExpandIndex = -1
            if (openAtIndex != -1) {
                var expand: Int? = openAtIndex
                while (expand != null) {
                    manager.expandDirectChildren(expand)
                    expand = manager.getParent(expand)
                }

                val hierarchyDescription = manager.getHierarchyDescription(openAtIndex)
                for (integer in hierarchyDescription) {
                    resultExpandIndex += integer!! + 1
                }
            }
            log("OutlineAdapter:: initializeTreeManager -- END $openAtIndex")
            return resultExpandIndex
        }
    }
}
