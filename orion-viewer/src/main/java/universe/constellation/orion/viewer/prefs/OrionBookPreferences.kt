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
import android.support.v7.app.AppCompatActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.DSLPreferenceFragment


/**
 * User: mike
 * Date: 17.05.12
 * Time: 21:41
 */
class OrionBookPreferencesFragment : DSLPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        rootScreen {
            title = R.string.book_pref_title.stringRes

            category {
                title = R.string.book_pref_title.stringRes

//                preference<OrionLayoutDialog> {
//                    key = "pageLayout"
//                    title = R.string.pref_page_layout.stringRes
//                    summary = R.string.pref_page_layout.stringRes
//                    dialogTitle = R.string.pref_page_layout.stringRes
//                    isCurrentBookOption = true
//                    setDefaultValue(0)
//                }

                preference<OrionListPreference> {
                    key = "walkOrder"
                    title = R.string.pref_walk_order.stringRes
                    summary = R.string.pref_walk_order.stringRes
                    dialogTitle = R.string.pref_walk_order.stringRes
                    isCurrentBookOption = true
                    setDefaultValue(R.string.ABCD.stringRes)
                    setDialogIcon(R.drawable.walk_order)

                    entries = R.array.walk_orders_desc.stringArray
                    entryValues = R.array.walk_orders.stringArray
                }

                preference<OrionListPreference> {
                    key = "screenOrientation"
                    title = R.string.pref_screen_orientation.stringRes
                    summary = R.string.pref_book_screen_orientation.stringRes
                    dialogTitle = R.string.pref_screen_orientation.stringRes
                    isCurrentBookOption = true
                    setDefaultValue("DEFAULT")

                    val isSDK9 = orionContext.sdkVersion >= Build.VERSION_CODES.GINGERBREAD
                    (if (isSDK9) R.array.screen_orientation_full_desc else R.array.screen_orientation_desc).stringArray.let {
                        it[0] = R.string.orientation_default_rotation.stringRes
                        entries = it
                    }
                    entryValues = (if (isSDK9) R.array.screen_orientation_full else R.array.screen_orientation).stringArray
                }

                preference<OrionListPreference> {
                    key = "colorMode"
                    title = R.string.pref_color_mode.stringRes
                    summary = R.string.pref_color_mode.stringRes
                    dialogTitle = R.string.pref_color_mode.stringRes
                    isCurrentBookOption = true
                    setDefaultValue("CM_NORMAL")

                    entries = R.array.color_mode_desc.stringArray
                    entryValues = R.array.color_mode.stringArray
                }

                preference<SeekBarPreference> {
                    key = "contrast"
                    title = R.string.pref_book_contrast.stringRes
                    summary = R.string.pref_book_contrast_desc.stringRes
                    isCurrentBookOption = true
                    minValue = 1
                    maxValue = 200
                    defaultSeekValue = 100
                }

                preference<SeekBarPreference> {
                    key = "threshold"
                    title = R.string.pref_book_threshold.stringRes
                    summary = R.string.pref_book_threshold_desc.stringRes
                    isCurrentBookOption = true
                    minValue = 1
                    maxValue = 255
                    defaultSeekValue = 255
                }
            }
        }
    }

    val orionContext: OrionApplication
        get() = context.applicationContext as OrionApplication

}

class OrionBookPreferences : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //orionContext.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, OrionBookPreferencesFragment())
                .commit()
    }

    val orionContext: OrionApplication
        get() = applicationContext as OrionApplication

}