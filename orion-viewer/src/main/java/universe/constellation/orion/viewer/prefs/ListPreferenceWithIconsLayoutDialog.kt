package universe.constellation.orion.viewer.prefs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import universe.constellation.orion.viewer.R

//copied from ListPreferenceDialogFragmentCompat
class ListPreferenceWithIconsLayoutDialog : PreferenceDialogFragmentCompat() {
    private var mClickedDialogEntryIndex = 0

    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null
    private var mIconsRes: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            check(!(preference.entries == null || preference.entryValues == null)) { "ListPreference requires an entries array and an entryValues array." }
            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
            mIconsRes = preference.iconsRes
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(
                SAVE_STATE_INDEX,
                0
            )
            mEntries =
                savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(
                SAVE_STATE_ENTRY_VALUES
            )
            mIconsRes = savedInstanceState.getIntArray(
                SAVE_STATE_ICONS
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            SAVE_STATE_INDEX,
            mClickedDialogEntryIndex
        )
        outState.putCharSequenceArray(
            SAVE_STATE_ENTRIES,
            mEntries
        )
        outState.putCharSequenceArray(
            SAVE_STATE_ENTRY_VALUES,
            mEntryValues
        )
        outState.putIntArray(SAVE_STATE_ICONS, mIconsRes)
    }

    private val listPreference: ListPreferenceWithIcons
        get() = getPreference() as ListPreferenceWithIcons

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setSingleChoiceItems(
            LayoutAdapter(
                requireContext(),
                R.layout.page_layout_pref,
                android.R.id.text1,
                mEntries!!,
                mIconsRes
            ), mClickedDialogEntryIndex
        ) { dialog, which ->
            mClickedDialogEntryIndex = which

            // Clicking on an item simulates the positive button click, and dismisses
            // the dialog.
            onClick(
                dialog,
                DialogInterface.BUTTON_POSITIVE
            )
            dialog.dismiss()
        }

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues!![mClickedDialogEntryIndex].toString()
            val preference = listPreference
            if (preference.callChangeListener(value)) {
                preference.setValue(value)
            }
        }
    }

    companion object {
        private const val SAVE_STATE_INDEX = "ListPreferenceWithIconsLayoutDialog.index"

        private const val SAVE_STATE_ENTRIES = "ListPreferenceWithIconsLayoutDialog.entries"

        private const val SAVE_STATE_ENTRY_VALUES =
            "ListPreferenceWithIconsLayoutDialog.entryValues"

        private const val SAVE_STATE_ICONS =
            "ListPreferenceWithIconsLayoutDialog.icons"

        fun newInstance(key: String): ListPreferenceWithIconsLayoutDialog {
            val fragment = ListPreferenceWithIconsLayoutDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.setArguments(b)
            return fragment
        }
    }


    class LayoutAdapter(
        context: Context,
        res: Int,
        textViewResourceId: Int,
        entriesText: Array<CharSequence>,
        private val iconsRes: IntArray?
    ) :
        ArrayAdapter<CharSequence>(context, res, textViewResourceId, entriesText) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).apply {
                val view = findViewById<CheckedTextView>(android.R.id.text1)
                view.text = ""
                val button = findViewById<ImageView>(R.id.ibutton)
                button.setImageResource(iconsRes?.getOrNull(position) ?: 0)
            }

        override fun hasStableIds(): Boolean {
            return true
        }
    }
}
