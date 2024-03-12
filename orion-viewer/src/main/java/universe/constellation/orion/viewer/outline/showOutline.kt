package universe.constellation.orion.viewer.outline

import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.polidea.treeview.InMemoryTreeStateManager
import pl.polidea.treeview.TreeViewList
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.outline.OutlineAdapter.Companion.initializeTreeManager

fun showOutline(controller: Controller, activity: OrionViewerActivity) {
    controller.runInScope {
        log("obtaining outline...")
        val outline = getOutline()

        withContext(Dispatchers.Main) {
            log("Show Outline...")
            if (outline.isNullOrEmpty()) {
                activity.showWarning(R.string.warn_no_outline)
                return@withContext
            }

            val dialog = AppCompatDialog(activity)
            dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.outline)
            val toolbar = dialog.findViewById<Toolbar>(R.id.toolbar)!!
            toolbar.setTitle(R.string.menu_outline_text)
            toolbar.setLogo(R.drawable.collapsed)
            val manager = InMemoryTreeStateManager<Int>()
            manager.setVisibleByDefault(false)
            val navigateTo = initializeTreeManager(manager, outline, controller.currentPage)
            val tocTree = dialog.findViewById<TreeViewList>(R.id.mainTreeView)
            tocTree!!.divider = ResourcesCompat.getDrawable(
                activity.resources,
                android.R.drawable.divider_horizontal_bright,
                null
            )
            tocTree.adapter = OutlineAdapter(controller, activity, dialog, manager, outline)
            tocTree.setSelection(navigateTo)
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()

            toolbar.setOnClickListener(object : View.OnClickListener {
                var expanded = false

                override fun onClick(v: View) {
                    if (expanded) {
                        toolbar.setLogo(R.drawable.collapsed)
                        val children = manager.getChildren(null)
                        for (child in children) {
                            manager.collapseChildren(child)
                        }
                    } else {
                        toolbar.setLogo(R.drawable.expanded)
                        manager.expandEverythingBelow(null)
                    }
                    expanded = !expanded
                }
            })
        }
    }
}