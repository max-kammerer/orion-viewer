package universe.constellation.orion.viewer.outline

import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.NavKind
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.log

private const val OUTLINE_TAB = 0
private const val HISTORY_TAB = 1

private const val DIALOG_WIDTH_FRACTION = 0.90f
private const val DIALOG_HEIGHT_FRACTION = 0.85f
private const val MAX_DIALOG_WIDTH_DP = 480
private const val MAX_DIALOG_HEIGHT_DP = 720
private const val DISABLED_ICON_ALPHA = 0.35f

fun showOutline(controller: Controller, activity: OrionViewerActivity) {
    controller.runInScope {
        log("obtaining outline...")
        val outline = getOutline()

        withContext(Dispatchers.Main) {
            log("Show Outline...")
            val hasOutline = !outline.isNullOrEmpty()
            val hasHistory = controller.history.canGoBack || controller.history.canGoForward
            if (!hasOutline && !hasHistory) {
                activity.showWarning(R.string.warn_no_outline)
                return@withContext
            }

            val dialog = AppCompatDialog(activity)
            dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.outline)

            val list = dialog.findViewById<RecyclerView>(R.id.mainTreeView)!!
            val emptyText = dialog.findViewById<TextView>(R.id.emptyText)!!
            val tabs = dialog.findViewById<TabLayout>(R.id.outlineTabs)!!
            val expandAll = dialog.findViewById<AppCompatImageButton>(R.id.expandAll)!!
            list.layoutManager = LinearLayoutManager(activity)
            list.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

            val outlineAdapter = if (hasOutline) {
                OutlineAdapter(outline!!, controller.currentPage) { item ->
                    try {
                        controller.goToPage(item.page)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        log(e)
                        activity.analytics.error(e, item.toString())
                        activity.showWarning(activity.getString(R.string.wrong_outline_item, e.message))
                    }
                }
            } else null

            var toolbarExpanded = false
            fun setExpandAllEnabled(enabled: Boolean) {
                expandAll.isEnabled = enabled
                expandAll.alpha = if (enabled) 1f else DISABLED_ICON_ALPHA
            }

            fun showOutlineTab() {
                setExpandAllEnabled(outlineAdapter != null)
                list.adapter = outlineAdapter
                emptyText.setText(R.string.warn_no_outline)
                emptyText.visibility = if (outlineAdapter == null) View.VISIBLE else View.GONE
                if (outlineAdapter != null && outlineAdapter.initialPosition >= 0) {
                    (list.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(outlineAdapter.initialPosition, list.paddingTop)
                }
            }

            fun showHistoryTab(history: HistoryAdapter) {
                setExpandAllEnabled(false)
                list.adapter = history
                emptyText.setText(R.string.outline_history_empty)
                emptyText.visibility = if (history.itemCount == 0) View.VISIBLE else View.GONE
                if (history.currentPosition >= 0) {
                    (list.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(history.currentPosition, list.paddingTop)
                }
            }

            fun createHistoryAdapter(): HistoryAdapter {
                return HistoryAdapter(
                    controller.history.backEntries(),
                    controller.currentPlace(),
                    controller.history.forwardEntries(),
                    outline
                ) { entry ->
                    val current = controller.currentPlace() ?: return@HistoryAdapter
                    val target = when (entry.kind) {
                        HistoryAdapter.Kind.BACK -> controller.history.backTo(entry.index, current)
                        HistoryAdapter.Kind.FORWARD -> controller.history.forwardTo(entry.index, current)
                        HistoryAdapter.Kind.CURRENT -> null
                    }
                    if (target != null) {
                        controller.goTo(target, NavKind.PAGING)
                        dialog.dismiss()
                    }
                }
            }

            tabs.addTab(tabs.newTab().setText(R.string.menu_outline_text), OUTLINE_TAB)
            tabs.addTab(tabs.newTab().setText(R.string.outline_tab_history), HISTORY_TAB)
            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        OUTLINE_TAB -> showOutlineTab()
                        HISTORY_TAB -> showHistoryTab(createHistoryAdapter())
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })

            if (hasOutline) {
                showOutlineTab()
            } else {
                tabs.getTabAt(HISTORY_TAB)?.select()
            }

            dialog.setCanceledOnTouchOutside(true)
            dialog.show()

            /* Fixed window size: content-driven sizing makes the dialog jump between the
             * long outline list and a short history one on tab switches. */
            val metrics = activity.resources.displayMetrics
            val maxWidth = (MAX_DIALOG_WIDTH_DP * metrics.density).toInt()
            val maxHeight = (MAX_DIALOG_HEIGHT_DP * metrics.density).toInt()
            dialog.window?.setLayout(
                minOf((metrics.widthPixels * DIALOG_WIDTH_FRACTION).toInt(), maxWidth),
                minOf((metrics.heightPixels * DIALOG_HEIGHT_FRACTION).toInt(), maxHeight)
            )

            expandAll.setOnClickListener {
                val adapter = outlineAdapter ?: return@setOnClickListener
                if (toolbarExpanded) {
                    adapter.collapseAll()
                } else {
                    adapter.expandAll()
                }
                toolbarExpanded = !toolbarExpanded
                expandAll.setImageResource(
                    if (toolbarExpanded) R.drawable.outline_collapse_all else R.drawable.outline_expand_all
                )
            }
        }
    }
}
