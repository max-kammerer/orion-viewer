/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
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

import android.os.Build
import android.os.Bundle
import android.preference.PreferenceScreen
import universe.constellation.orion.viewer.R
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
                title = if (!isGeneral) R.string.book_pref_title.stringRes else R.string.pref_default_book_setting.stringRes

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = SCREEN_ORIENTATION
                    title = R.string.pref_screen_orientation.stringRes
                    summary = R.string.pref_book_screen_orientation.stringRes
                    dialogTitle = R.string.pref_screen_orientation.stringRes
                    setDefaultValue("DEFAULT")

                    val isSDK9 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                    (if (isSDK9) R.array.screen_orientation_full_desc else R.array.screen_orientation_desc).stringArray.let {
                        if (!isGeneral) {
                            it[0] = R.string.orientation_default_rotation.stringRes
                        }
                        entries = it
                    }
                    entryValues = (if (isSDK9) R.array.screen_orientation_full else R.array.screen_orientation).stringArray
                }

                if (isGeneral) {
                    preference<OrionListPreference> {
                        isCurrentBookOption = !isGeneral
                        orionKey = ZOOM
                        title = R.string.pref_bookDefaultZoom.stringRes
                        summary = R.string.pref_bookDefaultZoom.stringRes
                        dialogTitle = R.string.pref_bookDefaultZoom.stringRes
                        setDefaultValue(0)

                        entries = R.array.default_zoom_option_desc.stringArray
                        entryValues = R.array.default_zoom_option.stringArray
                    }
                }

                preference<OrionLayoutDialog> {
                    isCurrentBookOption = !isGeneral
                    orionKey = PAGE_LAYOUT
                    title = R.string.pref_page_layout.stringRes
                    summary = R.string.pref_page_layout.stringRes
                    dialogTitle = R.string.pref_page_layout.stringRes
                    setDefaultValue(0)
                }

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = WALK_ORDER
                    title = R.string.pref_walk_order.stringRes
                    summary = R.string.pref_walk_order.stringRes
                    dialogTitle = R.string.pref_walk_order.stringRes
                    setDefaultValue(R.string.ABCD.stringRes)
                    setDialogIcon(R.drawable.walk_order)

                    entries = R.array.walk_orders_desc.stringArray
                    entryValues = R.array.walk_orders.stringArray
                }

                preference<OrionListPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = COLOR_MODE
                    title = R.string.pref_color_mode.stringRes
                    summary = R.string.pref_color_mode.stringRes
                    dialogTitle = R.string.pref_color_mode.stringRes
                    setDefaultValue("CM_NORMAL")

                    entries = R.array.color_mode_desc.stringArray
                    entryValues = R.array.color_mode.stringArray
                }

                preference<SeekBarPreference> {
                    isCurrentBookOption = !isGeneral
                    orionKey = CONTRAST
                    title = R.string.pref_book_contrast.stringRes
                    summary = R.string.pref_book_contrast_desc.stringRes

                    minValue = 1
                    maxValue = 200
                    defaultSeekValue = 100
                }

                if (!isGeneral) {
                    preference<SeekBarPreference> {
                        isCurrentBookOption = !isGeneral
                        orionKey = THRESHOLD
                        title = R.string.pref_book_threshold.stringRes
                        summary = R.string.pref_book_threshold_desc.stringRes

                        minValue = 1
                        maxValue = 255
                        defaultSeekValue = 255
                    }
                }
            }
        }
    }
}


