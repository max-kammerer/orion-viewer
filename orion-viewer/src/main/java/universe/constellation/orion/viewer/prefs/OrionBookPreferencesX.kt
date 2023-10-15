/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.prefs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.R.array.*
import universe.constellation.orion.viewer.R.string.*
import universe.constellation.orion.viewer.android.DSLPreferenceFragment
import universe.constellation.orion.viewer.prefs.BookPreferenceKeyX.*


class OrionBookPreferencesActivityX : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as OrionApplication).applyTheme(this)
        setContentView(R.layout.activity_with_fragment)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, OrionBookPreferencesFragment())
            .commit()

    }
}
class OrionBookPreferencesFragment : DSLPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //(applicationContext as OrionApplication).applyTheme(this)

        preferenceManager.preferenceDataStore = createDataStore(requireContext())
        rootScreen(requireContext()) {
            this.isIconSpaceReserved = false
            bookPreferences(this, false)
        }
    }

    companion object {
        fun DSLPreferenceFragment.bookPreferences(preferenceScreen: PreferenceScreen, isGeneral: Boolean) {

            preferenceScreen.category {
                title = (if (!isGeneral) book_pref_title else pref_default_book_setting).stringRes

                isIconSpaceReserved = false //TODO

                //preference<OrionListPreference> {
                list {
                    key = SCREEN_ORIENTATION.prefKey
                    title = pref_screen_orientation.stringRes
                    summary = pref_book_screen_orientation.stringRes
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
                        summary = pref_bookDefaultZoom.stringRes
                        dialogTitle = pref_bookDefaultZoom.stringRes
                        setDefaultValue("0")

                        entries = default_zoom_option_desc.stringArray
                        entryValues = default_zoom_option.stringArray
                    }
                }

//                TODO
                preference<OrionLayoutDialog> {
                    isCurrentBookOption = !isGeneral
                    orionKey = PAGE_LAYOUT
                    title = pref_page_layout.stringRes
                    summary = pref_page_layout.stringRes
                    dialogTitle = pref_page_layout.stringRes
                    setDefaultValue(0)
                }

//                preference<OrionListPreference> {
                list {
                    key = WALK_ORDER.prefKey
                    title = pref_walk_order.stringRes
                    summary = pref_walk_order.stringRes
                    dialogTitle = pref_walk_order.stringRes
                    setDefaultValue(ABCD.stringRes)
                    setDialogIcon(R.drawable.walk_order)

                    entries = walk_orders_desc.stringArray
                    entryValues = walk_orders.stringArray
                }

                list {
                    key = COLOR_MODE.prefKey
                    title = pref_color_mode.stringRes
                    summary = pref_color_mode.stringRes
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
        }
    }
}


