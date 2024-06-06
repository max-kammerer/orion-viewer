package universe.constellation.orion.viewer

import android.graphics.Rect
import universe.constellation.orion.viewer.selection.ClickInfo
import universe.constellation.orion.viewer.selection.ClickType
import universe.constellation.orion.viewer.selection.SelectionAutomata

enum class ContextAction(customName: String? = null) {

    //now just do word selection
    SELECT_TEXT {
        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val pos = clickInfo as? ClickInfo ?: return
            activity.selectionAutomata.selectText(
                true, false,
                SelectionAutomata.getSelectionRectangle(
                    pos.x,
                    pos.y,
                    0,
                    0,
                    true,
                    activity.controller?.pageLayoutManager ?: return
                ),
                Rect()
            )
        }
    },

    SELECT_WORD_AND_TRANSLATE {

        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val pos = clickInfo as? ClickInfo ?: return
            activity.selectionAutomata.selectText(
                true, true,
                SelectionAutomata.getSelectionRectangle(
                    pos.x,
                    pos.y,
                    0,
                    0,
                    true,
                    activity.controller?.pageLayoutManager ?: return
                ),
                Rect()
            )
        }
    },

    TAP_ACTION {
        override fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {
            val width = activity.view.sceneWidth
            val height = activity.view.sceneHeight
            if (height == 0 || width == 0) return

            val i = 3 * clickInfo.y / height
            val j = 3 * clickInfo.x / width

            val code = activity.globalOptions.getActionCode(i, j, clickInfo.clickType == ClickType.LONG)
            activity.doAction(code)
        }
    };

    val key = customName ?: name

    open fun doAction(activity: OrionViewerActivity, clickInfo: ClickInfo) {

    }

    companion object {
        fun findAction(action: String): ContextAction? {
            return entries.firstOrNull { it.key == action }
        }
    }
}