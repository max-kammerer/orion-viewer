package universe.constellation.orion.viewer.dialog

import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.TooltipCompat
import universe.constellation.orion.viewer.R

/**
 * Sets up the buttons under an option dialog, see option_dialog_bottom_panel.xml. [apply] is the
 * check mark: it applies and closes the dialog itself, as a failure may keep it open.
 *
 * A dialog that can try its values on the open book passes [preview] and [revert]: the eye
 * applies without closing, the arrow cancels, going back to the values the dialog opened with,
 * and closes. It has no close button: back or a tap outside closes it keeping what was
 * previewed. Other dialogs get close and apply only.
 */
fun AppCompatDialog.initOptionDialogPanel(
    apply: () -> Unit,
    preview: (() -> Unit)? = null,
    revert: (() -> Unit)? = null,
) {
    fun button(id: Int, action: (() -> Unit)?) {
        val button = findViewById<ImageButton>(id) ?: return
        button.visibility = if (action != null) View.VISIBLE else View.GONE
        if (action != null) {
            button.setOnClickListener { action() }
            //the long press tells what an icon does
            TooltipCompat.setTooltipText(button, button.contentDescription)
        }
    }

    button(R.id.option_dialog_bottom_revert, revert?.let { { it(); dismiss() } })
    button(R.id.option_dialog_bottom_preview, preview)
    button(R.id.option_dialog_bottom_close, if (preview == null) ({ dismiss() }) else null)
    button(R.id.option_dialog_bottom_apply, apply)
}
