package universe.constellation.orion.viewer.dialog

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatDialog
import android.view.View
import android.view.Window
import android.widget.*
import universe.constellation.orion.viewer.layout.CropMargins
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R

fun CropMargins.toDialogMargins() = intArrayOf(left, right, top, bottom, evenLeft, evenRight)

fun IntArray.toMargins(evenCrop: Boolean, cropMode: Int) =
        CropMargins(this[0], this[1], this[2], this[3], this[4], this[5], evenCrop, cropMode)

class CropDialog(cropMargins: CropMargins, val context: OrionViewerActivity) : AppCompatDialog(context) {

    val cropMargins = cropMargins.toDialogMargins()
    val evenCrop = cropMargins.evenCrop
    val cropMode = cropMargins.cropMode

    companion object {
        const val CROP_RESTRICTION_MIN = -10

        const val CROP_RESTRICTION_MAX = 40

        const val CROP_DELTA = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.crop_dialog)

        val viewAnimator = findViewById<View>(R.id.viewanim) as ViewAnimator
        val tabLayout = findViewById<View>(R.id.sliding_tabs) as TabLayout
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.crop))
        /*tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.crop))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.crop))*/

        tabLayout.addTab(tabLayout.newTab().setText("%2"))
        tabLayout.addTab(tabLayout.newTab().setText("Auto"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewAnimator.displayedChild = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        initCropScreen()
    }


    fun initCropScreen() {
        val generalCropTable = findViewById<View>(R.id.crop_borders) as TableLayout

        for (i in 0 until generalCropTable.childCount) {
            linkCropButtonsAndText(i, generalCropTable.getChildAt(i) as TableRow)
        }

        //even cropping
        val evenCropTable = findViewById<View>(R.id.crop_borders_even) as TableLayout
        var index = 4
        for (i in 0 until evenCropTable.childCount) {
            val child = evenCropTable.getChildAt(i)
            if (child is TableRow) {
                linkCropButtonsAndText(index, child)
                index++

                for (j in 0 until child.childCount) {
                    val v = child.getChildAt(j)
                    v.isEnabled = false
                }
            }
        }

        val checkBox = findViewById<View>(R.id.crop_even_flag) as CheckBox
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            for (i in 0 until evenCropTable.childCount) {
                val child = evenCropTable.getChildAt(i)
                if (child is TableRow) {
                    for (j in 0 until child.childCount) {
                        child.getChildAt(j).isEnabled = isChecked
                    }
                }
            }
        }

        val preview = findViewById<View>(R.id.crop_preview) as ImageButton
        val radioGroup = findViewById<View>(R.id.crop_mode) as RadioGroup

        preview.setOnClickListener {
            context.onAnimatorCancel()
            val radioButtonId = radioGroup.checkedRadioButtonId
            val radioButton = radioGroup.findViewById<View>(radioButtonId)
            val mode = radioGroup.indexOfChild(radioButton)
            context.controller?.changeCropMargins(cropMargins.toMargins(checkBox.isChecked, mode))
        }

        val close = findViewById<View>(R.id.crop_close) as ImageButton
        close.setOnClickListener({
            dismiss()
        })

        updateView()
    }

    private fun linkCropButtonsAndText(i: Int, row: TableRow) {
        val valueView = row.findViewById<View>(R.id.crop_value) as TextView
        val plus = row.findViewById<View>(R.id.crop_plus) as ImageButton
        val minus = row.findViewById<View>(R.id.crop_minus) as ImageButton
        linkCropButtonsAndText(minus, plus, valueView, i)
    }

    fun linkCropButtonsAndText(minus: ImageButton, plus: ImageButton, text: TextView, cropIndex: Int) {
        minus.setOnClickListener {
            if (cropMargins[cropIndex] != CROP_RESTRICTION_MIN) {
                cropMargins[cropIndex] = cropMargins[cropIndex] - 1
                text.text = "${cropMargins[cropIndex]}%"
            }
        }

        minus.setOnLongClickListener {
            cropMargins[cropIndex] = cropMargins[cropIndex] - CROP_DELTA
            if (cropMargins[cropIndex] < CROP_RESTRICTION_MIN) {
                cropMargins[cropIndex] = CROP_RESTRICTION_MIN
            }
            text.text = "${cropMargins[cropIndex]}%"
            true
        }

        plus.setOnClickListener {
            cropMargins[cropIndex] = cropMargins[cropIndex] + 1
            if (cropMargins[cropIndex] > CROP_RESTRICTION_MAX) {
                cropMargins[cropIndex] = CROP_RESTRICTION_MAX
            }
            text.text = "${cropMargins[cropIndex]}%"
        }

        plus.setOnLongClickListener {
            cropMargins[cropIndex] = cropMargins[cropIndex] + CROP_DELTA
            if (cropMargins[cropIndex] > CROP_RESTRICTION_MAX) {
                cropMargins[cropIndex] = CROP_RESTRICTION_MAX
            }
            text.text = "${cropMargins[cropIndex]}%"
            true
        }
    }

    fun updateView() {
        val cropTable = findViewById<View>(R.id.crop_borders) as TableLayout
        for (i in 0 until cropTable.childCount) {
            val row = cropTable.getChildAt(i) as TableRow
            val valueView = row.findViewById<View>(R.id.crop_value) as TextView
            valueView.text = "${cropMargins[i]}%"
        }

        val cropTable2 = findViewById<View>(R.id.crop_borders_even) as TableLayout
        var index = 4
        for (i in 0 until cropTable2.childCount) {
            if (cropTable2.getChildAt(i) is TableRow) {
                val row = cropTable2.getChildAt(i) as TableRow
                val valueView = row.findViewById<View>(R.id.crop_value) as TextView
                valueView.text = "${cropMargins[index]}%"
                index++
            }
        }
        (findViewById<View>(R.id.crop_even_flag) as CheckBox).isChecked = evenCrop

        val radioGroup = findViewById<View>(R.id.crop_mode) as RadioGroup
        (radioGroup.getChildAt(cropMode) as RadioButton).isChecked = true
    }

}

fun create(context: OrionViewerActivity, cropMargins: CropMargins): CropDialog {
    val appCompatDialog = CropDialog(cropMargins, context)
    appCompatDialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    return appCompatDialog
}
