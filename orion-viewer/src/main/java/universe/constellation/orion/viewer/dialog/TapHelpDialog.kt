package universe.constellation.orion.viewer.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.analytics.TAP_HELP_DIALOG
import universe.constellation.orion.viewer.prefs.OrionTapActivity.Companion.getDefaultAction
import universe.constellation.orion.viewer.dpToPixels

class TapHelpDialog : DialogFragment(R.layout.tap)  {

    override fun getTheme() = android.R.style.Theme_Translucent


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireDialog().setTitle(R.string.tap_zones_header)

        val typedValue = requireContext().obtainStyledAttributes(
            R.style.Tap_Dialog_Top,
            intArrayOf(android.R.attr.textColor)
        )
        val color = typedValue.getColor(0, 0xFF000000u.toInt())
        typedValue.recycle()

        val size = requireContext().dpToPixels(24f)

        val table = view.findViewById<TableLayout>(R.id.tap_table)
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val layout = row.getChildAt(j) as RelativeLayout
                val clickActionTextView = layout.findViewById<TextView>(R.id.shortClick)
                val longPressActionTextView = layout.findViewById<TextView>(R.id.longClick)
                longPressActionTextView.visibility = View.GONE

                val singleClickAction = Action.getAction(getDefaultAction(i, j, false))

                val isNext = singleClickAction === Action.NEXT
                val isPrev = singleClickAction === Action.PREV

                layout.setBackgroundColor((0xffffcc66u).toInt())
                clickActionTextView.text = requireActivity().getString(singleClickAction.getName())

                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                clickActionTextView.setLayoutParams(layoutParams)

                setImages(isNext, isPrev, layout, color, size)
            }
        }

        view.setOnClickListener {
            dismiss()
        }
    }

    private fun setImages(
        isNext: Boolean,
        isPrev: Boolean,
        layout: RelativeLayout,
        color: Int,
        size: Int
    ) {
        val imageView = createImageView(isNext, isPrev, color)

        val imageLayout = RelativeLayout.LayoutParams(
            size,
            size,
        )
        imageLayout.addRule(RelativeLayout.BELOW, R.id.shortClick)
        imageLayout.addRule(RelativeLayout.CENTER_HORIZONTAL)

        imageView.setLayoutParams(imageLayout)
        layout.addView(imageView)
    }

    private fun createImageView(
        isNext: Boolean,
        isPrev: Boolean,
        color: Int
    ): AppCompatImageView {
        val imageView = AppCompatImageView(requireContext())
        val res =
            if (isNext) R.drawable.tap_help_forward else if (isPrev) R.drawable.tap_help_back else R.drawable.new_outline

        imageView.setImageResource(res)
        imageView.setColorFilter((color or (255 shl 24)))

        return imageView
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? OrionViewerActivity)?.analytics?.dialog(TAP_HELP_DIALOG, false)
    }
}
