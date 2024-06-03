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

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import universe.constellation.orion.viewer.OptionActions
import universe.constellation.orion.viewer.PageOptions
import universe.constellation.orion.viewer.PageWalker
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.prefs.OrionApplication.Companion.instance
import java.io.Serializable

class GlobalOptions(
    context: OrionApplication,
    prefs: SharedPreferences
) : PreferenceWrapper(prefs), Serializable, PageOptions {
    var recentFiles = mutableListOf<RecentEntry>()

    private val registeredPreferences = mutableMapOf<String, Preference<*>>()

    /* Caution: The preference manager does not currently store a strong reference to the listener.
    You must store a strong reference to the listener, or it will be susceptible to garbage collection.
    We recommend you keep a reference to the listener in the instance data of an object that will exist as long as you need the listener. */
    private val onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener

    init {
        for (i in 0 until MAX_RECENT_ENTRIES) {
            val entry = prefs.getString(RECENT_PREFIX + i, null)
            if (entry == null) {
                break
            } else {
                recentFiles.add(RecentEntry(entry))
            }
        }

        //TODO ?
        onSharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { preferences1: SharedPreferences, name: String? ->
                log("onSharedPreferenceChanged $name")

                registeredPreferences[name]?.update()?.also {
                    return@OnSharedPreferenceChangeListener
                }

                val activity = context.viewActivity
                if (activity != null) {
                    if (SHOW_STATUS_BAR == name) {
                        OptionActions.SHOW_STATUS_BAR.doAction(activity, false, isStatusBarVisible)
                    } else if (SHOW_OFFSET_ON_STATUS_BAR == name) {
                        OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(
                            activity,
                            false,
                            isShowOffsetOnStatusBar
                        )
                    } else if (SCREEN_OVERLAPPING_HORIZONTAL == name) {
                        OptionActions.SCREEN_OVERLAPPING_HORIZONTAL.doAction(
                            activity,
                            horizontalOverlapping,
                            verticalOverlapping
                        )
                    } else if (SCREEN_OVERLAPPING_VERTICAL == name) {
                        OptionActions.SCREEN_OVERLAPPING_VERTICAL.doAction(
                            activity,
                            horizontalOverlapping,
                            verticalOverlapping
                        )
                    } else if (APP_LANGUAGE == name) {
                        context.setLanguage(appLanguage)
                    } else if (DRAW_OFF_PAGE == name) {
                        activity.fullScene.setDrawOffPage(isDrawOffPage)
                        //TODO ?
                        activity.view.invalidate()
                    } else if (OptionActions.SHOW_TIME_ON_STATUS_BAR.key == name) {
                        OptionActions.SHOW_TIME_ON_STATUS_BAR.doAction(
                            activity,
                            !isShowClockOnStatusBar,
                            isShowClockOnStatusBar
                        )
                    }
                }

                if (DEBUG == name) {
                    context.startOrStopDebugLogger(getBooleanProperty(DEBUG, false))
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }


    val lastOpenedDirectory: String?
        get() = getNullableStringProperty(OrionFileManagerActivity.LAST_OPENED_DIRECTORY, null)

    fun addRecentEntry(newEntry: RecentEntry) {
        val iterator = recentFiles.iterator()
        while (iterator.hasNext()) {
            val recentEntry = iterator.next()
            if (recentEntry.path == newEntry.path) {
                iterator.remove()
                break
            }
        }

        recentFiles.add(0, newEntry)

        if (recentFiles.size > MAX_RECENT_ENTRIES) {
            recentFiles.removeLast()
        }
    }

    fun saveRecents() {
        var i = 0
        val editor = prefs.edit()
        val iterator: Iterator<RecentEntry> = recentFiles.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            editor.putString(RECENT_PREFIX + i, next.path)
            i++
        }
        editor.apply()
    }

    class RecentEntry(val path: String) : Serializable {
        val lastPathElement: String
            get() = path.substring(path.lastIndexOf("/") + 1)

        override fun toString(): String {
            return lastPathElement
        }
    }


    val isSwapKeys: Boolean
        get() = getBooleanProperty(SWAP_KEYS, false)

    val isEnableTouchMove: Boolean
        get() = getBooleanProperty(ENABLE_TOUCH_MOVE, true)

    val isEnableMoveOnPinchZoom: Boolean
        get() = getBooleanProperty(ENABLE_MOVE_ON_PINCH_ZOOM, false)

    val defaultZoom: Int
        get() = getIntFromStringProperty(DEFAULT_ZOOM, 0)

    val defaultContrast: Int
        get() = getIntFromStringProperty(DEFAULT_CONTRAST, 100)

    val isApplyAndClose: Boolean
        get() = getBooleanProperty(APPLY_AND_CLOSE, false)

    val isDrawOffPage: Boolean
        get() = getBooleanProperty(DRAW_OFF_PAGE, instance.device !is EInkDevice)

    val isActionBarVisible: Boolean
        get() = getBooleanProperty(SHOW_ACTION_BAR.key, SHOW_ACTION_BAR.defaultValue)

    val isShowTapHelp: Boolean
        get() = getBooleanProperty(SHOW_TAP_HELP, true)

    val isStatusBarVisible: Boolean
        get() = getBooleanProperty(SHOW_STATUS_BAR, true)

    val isNewUI: Boolean
        get() = !getBooleanProperty(OLD_UI, false)

    val isShowOffsetOnStatusBar: Boolean
        get() = getBooleanProperty(SHOW_OFFSET_ON_STATUS_BAR, true)

    val isShowClockOnStatusBar: Boolean
        get() = getBooleanProperty(SHOW_TIME_ON_STATUS_BAR, true)

    fun getActionCode(i: Int, j: Int, isLong: Boolean): Int {
        val key = OrionTapActivity.getKey(i, j, isLong)
        var code = getInt(key, -1)
        if (code == -1) {
            code = getInt(key, OrionTapActivity.getDefaultAction(i, j, isLong))
        }
        return code
    }

    val dictionary: String
        get() = getStringProperty(DICTIONARY, "FORA")

    val einkRefreshAfter: Int
        get() = getIntFromStringProperty(EINK_TOTAL_AFTER, 10)

    val isEinkOptimization: Boolean
        get() = getBooleanProperty(EINK_OPTIMIZATION, false)

    val longCrop: Int
        get() = getIntFromStringProperty(LONG_CROP_VALUE, 10)

    override val verticalOverlapping: Int
        get() = getIntFromStringProperty(SCREEN_OVERLAPPING_VERTICAL, 3)

    override val horizontalOverlapping: Int
        get() = getIntFromStringProperty(SCREEN_OVERLAPPING_HORIZONTAL, 3)

    val brightness: Int
        get() = getIntFromStringProperty(BRIGHTNESS, 100)

    val isCustomBrightness: Boolean
        get() = getBooleanProperty(CUSTOM_BRIGHTNESS, false)

    val isOpenRecentBook: Boolean
        get() = getBooleanProperty(OPEN_RECENT_BOOK, false)


    val applicationTheme: String
        get() = getStringProperty(APPLICATION_THEME, APPLICATION_THEME_DEFAULT)

    val appLanguage: String
        get() = getStringProperty(APP_LANGUAGE, DEFAULT_LANGUAGE)

    val walkOrder: String
        get() = getStringProperty(WALK_ORDER, PageWalker.WALK_ORDER.ABCD.name)

    val pageLayout: Int
        get() = getInt(PAGE_LAYOUT, 0)

    val colorMode: String
        get() = getStringProperty(COLOR_MODE, "CM_NORMAL")

    val SCREEN_BACKLIGHT_TIMEOUT = pref("SCREEN_BACKLIGHT_TIMEOUT", 10, stringAsInt = true)

    val FULL_SCREEN= pref("FULL_SCREEN", false)

    val SHOW_ACTION_BAR = pref("SHOW_ACTION_BAR", true)

    fun <T> subscribe(pref: Preference<T>) {
        registeredPreferences.put(pref.key, pref)?.also {
            errorInDebug("Pref with key ${pref.key} already registered: $pref ")
        }
    }

    companion object {
        const val MAX_RECENT_ENTRIES: Int = 20

        private const val RECENT_PREFIX = "recent_"

        const val SWAP_KEYS: String = "SWAP_KEYS"

        const val DEFAULT_ZOOM: String = "DEFAULT_ZOOM"

        const val DEFAULT_CONTRAST: String = "DEFAULT_CONTRAST_3"

        const val APPLY_AND_CLOSE: String = "APPLY_AND_CLOSE"

        const val FULL_SCREEN: String = "FULL_SCREEN"

        const val DRAW_OFF_PAGE: String = "DRAW_OFF_PAGE"

        const val SHOW_ACTION_BAR: String = "SHOW_ACTION_BAR"

        const val SHOW_STATUS_BAR: String = "SHOW_STATUS_BAR"

        const val OLD_UI: String = "OLD_UI"

        const val SHOW_OFFSET_ON_STATUS_BAR: String = "SHOW_OFFSET_ON_STATUS_BAR"

        const val SHOW_TIME_ON_STATUS_BAR: String = "SHOW_TIME_ON_STATUS_BAR"

        const val TAP_ZONE: String = "TAP_ZONE"

        const val SCREEN_ORIENTATION: String = "SCREEN_ORIENTATION"

        const val EINK_OPTIMIZATION: String = "EINK_OPTIMIZATION"

        const val EINK_TOTAL_AFTER: String = "EINK_TOTAL_AFTER"

        const val DICTIONARY: String = "DICTIONARY"

        const val LONG_CROP_VALUE: String = "LONG_CROP_VALUE"

        const val SCREEN_OVERLAPPING_HORIZONTAL: String = "SCREEN_OVERLAPPING_HORIZONTAL"

        const val SCREEN_OVERLAPPING_VERTICAL: String = "SCREEN_OVERLAPPING_VERTICAL"

        const val DEBUG: String = "DEBUG"

        const val BRIGHTNESS: String = "BRIGHTNESS"

        const val CUSTOM_BRIGHTNESS: String = "CUSTOM_BRIGHTNESS"

        const val APPLICATION_THEME: String = "APPLICATION_THEME"

        const val APPLICATION_THEME_DEFAULT: String = "DEFAULT"

        const val APP_LANGUAGE: String = "LANGUAGE"

        const val OPEN_RECENT_BOOK: String = "OPEN_RECENT_BOOK"

        const val DAY_NIGHT_MODE: String = "DAY_NIGHT_MODE"

        const val WALK_ORDER: String = "WALK_ORDER"

        const val PAGE_LAYOUT: String = "PAGE_LAYOUT"

        const val COLOR_MODE: String = "COLOR_MODE"

        const val SHOW_TAP_HELP: String = "SHOW_TAP_HELP"

        const val TEST_SCREEN_WIDTH: String = "TEST_SCREEN_WIDTH"

        const val TEST_SCREEN_HEIGHT: String = "TEST_SCREEN_HEIGHT"

        const val OPEN_AS_TEMP_BOOK: String = "OPEN_AS_TEMP_BOOK"

        const val ENABLE_TOUCH_MOVE: String = "ENABLE_TOUCH_MOVE"

        const val ENABLE_MOVE_ON_PINCH_ZOOM: String = "ENABLE_MOVE_ON_PINCH_ZOOM"

        const val VERSION: String = "VERSION"

        const val DEFAULT_LANGUAGE: String = "DEFAULT"
    }
}
