package universe.constellation.orion.viewer.prefs

import android.annotation.SuppressLint
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import universe.constellation.orion.viewer.PageNumbering
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.R.array.*
import universe.constellation.orion.viewer.R.string.*
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.prefs.BookPreferenceKeyX.*
import universe.constellation.orion.viewer.prefs.GlobalOptions.Companion.DEFAULT_DICTIONARY


class OrionBookPreferencesActivityX : OrionBaseActivity() {
    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.activity_with_fragment, true, true)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, OrionBookPreferencesFragment())
                .commit()
        }
    }
}

class OrionBookPreferencesFragment : DSLPreferenceFragment() {

    private val onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { preference, key ->
            val context = context ?: return@OnSharedPreferenceChangeListener
            if (preference == null) return@OnSharedPreferenceChangeListener
            if (BookPreferenceKeyX.key2Operation[key] != null) {
                val dataStore = createDataStore(context)
                if (key == THRESHOLD.prefKey || key == PAGE_LAYOUT.prefKey) {
                    dataStore.putInt(key, preference.getInt(key, 0))
                } else {
                    dataStore.putString(key, preference.getString(key, null))
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val isGeneral = requireActivity() is OrionPreferenceActivityX

        if (!isGeneral) {
            preferenceManager.preferenceDataStore = createDataStore(requireContext())
        }

        rootScreen(requireContext(), isGeneral) {
            this.isIconSpaceReserved = false
            bookPreferences(this, isGeneral)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val isGeneral = requireActivity() is OrionPreferenceActivityX
        if (isGeneral) {
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val isGeneral = requireActivity() is OrionPreferenceActivityX
        if (isGeneral) {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
        }
    }

    companion object {
        /**
         * The book stores [PageNumbering.offset], but the user is asked for the number printed on
         * the page being read: no arithmetic, and it stays right for scanned spreads.
         */
        private fun DSLPreferenceFragment.pageNumberingPreferences(group: PreferenceGroup) {
            val app = requireContext().applicationContext as OrionApplication
            val store = createDataStore(requireContext())

            fun docPage() = app.viewActivity?.controller?.currentPage ?: app.currentBookParameters?.pageNumber ?: 0
            fun numbering(pagesPerSheet: Int = store.getInt(PAGES_PER_SHEET.prefKey, 1)) =
                PageNumbering(store.getInt(LOGICAL_PAGE_OFFSET.prefKey, 0), pagesPerSheet)
            fun refreshNumberingSummary() {
                //setting the provider is the public way to make the preference re-read it
                findPreference<EditTextPreference>(LOGICAL_PAGE_OFFSET.prefKey)?.let { it.summaryProvider = it.summaryProvider }
            }

            with(group) {
                list {
                    key = PAGES_PER_SHEET.prefKey
                    title = pref_book_pages_per_sheet.stringRes
                    summary = pref_book_pages_per_sheet_desc.stringRes
                    dialogTitle = pref_book_pages_per_sheet.stringRes
                    setDefaultValue("1")

                    entries = arrayOf(pref_book_pages_per_sheet_one.stringRes, pref_book_pages_per_sheet_two.stringRes)
                    entryValues = arrayOf("1", "2")

                    setOnPreferenceChangeListener { _, newValue ->
                        val old = numbering()
                        val new = numbering((newValue as String).toInt())
                        if (old.offset != 0 && old.isSpread != new.isSpread) {
                            //the offset counts book pages: keep the numbers the user has set up for the current page
                            store.putInt(LOGICAL_PAGE_OFFSET.prefKey, new.offsetFor(docPage(), old.firstOn(docPage())))
                        }
                        //the new value is stored after the listener
                        view?.post { refreshNumberingSummary() }
                        true
                    }
                }

                preference(EditTextPreference(context)) {
                    key = LOGICAL_PAGE_OFFSET.prefKey
                    //the field takes a printed page number, the offset behind it is stored by the listener
                    isPersistent = false
                    title = pref_book_page_numbering.stringRes
                    dialogTitle = pref_book_page_numbering.stringRes
                    dialogMessage = pref_book_page_numbering_message.stringRes
                    summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                        getString(pref_book_page_numbering_desc, numbering().sheetLabel(docPage()), docPage() + 1)
                    }

                    setOnBindEditTextListener { editText ->
                        editText.inputType = InputType.TYPE_CLASS_NUMBER
                        editText.setText(numbering().firstOn(docPage()).takeIf { it >= 1 }?.toString() ?: "")
                        editText.selectAll()
                    }

                    setOnPreferenceChangeListener { _, newValue ->
                        val text = (newValue as String).trim()
                        val printedNumber = text.toIntOrNull()
                        if (text.isEmpty() || printedNumber != null) {
                            store.putInt(
                                LOGICAL_PAGE_OFFSET.prefKey,
                                if (printedNumber == null) 0 else numbering().offsetFor(docPage(), printedNumber)
                            )
                            refreshNumberingSummary()
                        }
                        false
                    }
                }
            }
        }

        fun DSLPreferenceFragment.bookPreferences(preferenceScreen: PreferenceScreen, isGeneral: Boolean) {

            preferenceScreen.category {
                isIconSpaceReserved = false //TODO

                list {
                    key = DICTIONARY.prefKey
                    title = pref_dictionary.stringRes
                    summary = pref_dictionary_desc.stringRes
                    dialogTitle = pref_dictionary.stringRes

                    if (!isGeneral) {
                        setDefaultValue("DEFAULT")
                        entries = arrayOf("DEFAULT", *dictionaries_desc.stringArray)
                        entryValues = arrayOf("DEFAULT", *dictionaries.stringArray)
                    } else {
                        setDefaultValue(DEFAULT_DICTIONARY)
                        entries = dictionaries_desc.stringArray
                        entryValues = dictionaries.stringArray
                    }
                }

                //preference<OrionListPreference> {
                list {
                    key = SCREEN_ORIENTATION.prefKey
                    title = pref_screen_orientation.stringRes
                    summary = if (!isGeneral) pref_book_screen_orientation_desc.stringRes else pref_screen_orientation_desc.stringRes
                    dialogTitle = pref_screen_orientation.stringRes
                    setDefaultValue("DEFAULT")

                    screen_orientation_full_desc.stringArray.let {
                        if (!isGeneral) {
                            it[0] = orientation_default_rotation.stringRes
                        }
                        entries = it
                    }
                    entryValues = screen_orientation_full.stringArray
                }

                if (isGeneral) {
                    //preference<OrionListPreference> {
                    list {
                        key = ZOOM.prefKey
                        title = pref_bookDefaultZoom.stringRes
                        summary = pref_bookDefaultZoom_desc.stringRes
                        dialogTitle = pref_bookDefaultZoom.stringRes
                        setDefaultValue("0")

                        entries = default_zoom_option_desc.stringArray
                        entryValues = default_zoom_option.stringArray
                    }
                }

                intListWithIcons {
                    key = PAGE_LAYOUT.prefKey
                    title = pref_page_layout.stringRes
                    summary = pref_page_layout.stringRes
                    dialogTitle = pref_page_layout.stringRes
                    setDefaultValue("0")

                    entries = arrayOf("", "", "")
                    entryValues = arrayOf("0", "1", "2")
                    iconsRes = intArrayOf(R.drawable.navigation1, R.drawable.navigation2, R.drawable.navigation3)
                }

//                preference<OrionListPreference> {
                list {
                    key = WALK_ORDER.prefKey
                    title = pref_walk_order.stringRes
                    summary = pref_walk_order_desc.stringRes
                    dialogTitle = pref_walk_order.stringRes
                    setDefaultValue(ABCD.stringRes)
                    setDialogIcon(R.drawable.walk_order)

                    entries = walk_orders_desc.stringArray
                    entryValues = walk_orders.stringArray
                }

                list {
                    key = COLOR_MODE.prefKey
                    title = pref_color_mode.stringRes
                    summary = pref_color_mode_desc.stringRes
                    dialogTitle = pref_color_mode.stringRes
                    setDefaultValue("CM_NORMAL")

                    entries = color_mode_desc.stringArray
                    entryValues = color_mode.stringArray
                }

                list {
                    key = CONTRAST.prefKey
                    title = pref_book_contrast.stringRes
                    summary = pref_book_contrast_desc.stringRes

                    val values = Array(17) { index ->
                        when {
                            index <= 5 -> 10 + index * 15
                            index <= 12 -> 100 + (index - 6) * 50
                            else -> 500 + (index - 12) * 100
                        }.toString()
                    }

                    entries = values
                    entryValues = values
                    setDefaultValue("100")
                }

                if (!isGeneral) {
                    //preference<SeekBarPreference> {
                    seekBar {
                        //isCurrentBookOption = !isGeneral
                        key = THRESHOLD.prefKey
                        title = pref_book_threshold.stringRes
                        summary = pref_book_threshold_desc.stringRes
                        showSeekBarValue = true

                        min = 1
                        max = 255
                        setDefaultValue(255)
                    }
                }
            }

            if (!isGeneral) {
                preferenceScreen.category {
                    title = pref_book_logical_pages.stringRes
                    isIconSpaceReserved = false

                    pageNumberingPreferences(this)
                }
            }
        }
    }
}


