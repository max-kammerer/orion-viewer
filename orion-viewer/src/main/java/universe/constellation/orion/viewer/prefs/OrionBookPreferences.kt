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
import android.preference.PreferenceScreen
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.R.array.*
import universe.constellation.orion.viewer.R.string.*
import universe.constellation.orion.viewer.android.DSLPreferenceActivity
import universe.constellation.orion.viewer.prefs.BookPreferenceKey.*


class OrionBookPreferences : DSLPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as OrionApplication).applyTheme(this)

        super.onCreate(savedInstanceState)

        rootScreen {
            bookPreferences(this, false)
        }
    }


    companion object {
        fun DSLPreferenceActivity.bookPreferences(preferenceScreen: PreferenceScreen, isGeneral: Boolean) {
            preferenceScreen.category {
                title = (if (!isGeneral) book_pref_title else pref_default_book_setting).stringRes

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = SCREEN_ORIENTATION
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
                    preference<OrionListPreference> {
                        isCurrentBookOption = !isGeneral
                        orionKey = ZOOM
                        title = pref_bookDefaultZoom.stringRes
                        summary = pref_bookDefaultZoom.stringRes
                        dialogTitle = pref_bookDefaultZoom.stringRes
                        setDefaultValue("0")

                        entries = default_zoom_option_desc.stringArray
                        entryValues = default_zoom_option.stringArray
                    }
                }

                preference<OrionLayoutDialog> {
                    isCurrentBookOption = !isGeneral
                    orionKey = PAGE_LAYOUT
                    title = pref_page_layout.stringRes
                    summary = pref_page_layout.stringRes
                    dialogTitle = pref_page_layout.stringRes
                    setDefaultValue(0)
                }

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = WALK_ORDER
                    title = pref_walk_order.stringRes
                    summary = pref_walk_order.stringRes
                    dialogTitle = pref_walk_order.stringRes
                    setDefaultValue(ABCD.stringRes)
                    setDialogIcon(R.drawable.walk_order)

                    entries = walk_orders_desc.stringArray
                    entryValues = walk_orders.stringArray
                }

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = COLOR_MODE
                    title = pref_color_mode.stringRes
                    summary = pref_color_mode.stringRes
                    dialogTitle = pref_color_mode.stringRes
                    setDefaultValue("CM_NORMAL")

                    entries = color_mode_desc.stringArray
                    entryValues = color_mode.stringArray
                }

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = CONTRAST
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
                    preference<SeekBarPreference> {
                        isCurrentBookOption = !isGeneral
                        orionKey = THRESHOLD
                        title = pref_book_threshold.stringRes
                        summary = pref_book_threshold_desc.stringRes

                        minValue = 1
                        maxValue = 255
                        defaultSeekValue = 255
                    }
                }
            }
        }
    }
}


