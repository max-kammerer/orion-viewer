package universe.constellation.orion.viewer

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class MainMenuFragment : Fragment(R.layout.new_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initChildActions(view, R.id.menu_top_actions)
        initChildActions(view, R.id.menu_botton_actions)

        view.findViewById<LinearLayout>(R.id.menu_middle_part).setOnClickListener {
            hide()
        }
    }

    private fun initChildActions(view: View, id: Int) {
        val panel = view.findViewById<LinearLayout>(id)
        for (i in 0 until panel.childCount) {
            val child = panel.getChildAt(i)
            if (child is ImageView) {
                child.setOnClickListener {
                    doClick(it.id)
                }
            }
        }
    }

    private fun doClick(id: Int) {
        hide()
        val orionViewerActivity = requireActivity() as OrionViewerActivity
        val menuAction = orionViewerActivity.getMenuAction(id)
        orionViewerActivity.doAction(menuAction)
    }

    private fun hide() {
        (activity as OrionViewerActivity).hideMenu()
    }
}