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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.preference.PreferenceManager
import org.jetbrains.annotations.VisibleForTesting
import universe.constellation.orion.viewer.AndroidLogger
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.BuildConfig.BUILD_TYPE
import universe.constellation.orion.viewer.BuildConfig.VERSION_NAME
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.analytics.Analytics
import universe.constellation.orion.viewer.bookmarks.BookmarkAccessor
import universe.constellation.orion.viewer.device.AndroidDevice
import universe.constellation.orion.viewer.device.EInkDeviceWithoutFastRefresh
import universe.constellation.orion.viewer.device.MagicBookBoeyeDevice
import universe.constellation.orion.viewer.device.OnyxDevice
import universe.constellation.orion.viewer.device.OnyxUtil
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.logger
import universe.constellation.orion.viewer.test.IdlingResource
import java.util.Locale
import kotlin.properties.Delegates

class OrionApplication : Application() {

    internal var idlingRes = IdlingResource()

    val options: GlobalOptions by lazy {
        GlobalOptions(this, PreferenceManager.getDefaultSharedPreferences(this), true)
    }

    val keyBinding: GlobalOptions by lazy {
        GlobalOptions(this, getSharedPreferences("key_binding", Context.MODE_PRIVATE), false)
    }

    val analytics: Analytics by lazy  { Analytics.initialize(contentResolver, BuildConfig.ANALYTICS) }

    var tempOptions: TemporaryOptions? = null
        private set

    private var bookmarkAccessor: BookmarkAccessor? = null

    var viewActivity: OrionViewerActivity? = null

    var currentBookParameters: LastPageInfo? = null

    val device = createDevice()

    private var langCode: String? = null

    private val isLightTheme: Boolean
        get() {
            val theme = options.applicationTheme
            val isDefault = !("DARK" == theme || "LIGHT" == theme)
            val useDarkTheme = if (isDefault) device.isDefaultDarkTheme else false

            return !(useDarkTheme || "DARK" == theme)

        }

    private val themeId: Int
        get() = if (!isLightTheme)
            R.style.Theme_AppCompat_NoActionBar
        else
            R.style.Theme_AppCompat_Light_NoActionBar

    val sdkVersion: Int
        get() = Build.VERSION.SDK_INT

    override fun onCreate() {
        logger = AndroidLogger
        instance = this
        super.onCreate()
        setLangCode(options.appLanguage)
        logOrionAndDeviceInfo()
        if (device is EInkDeviceWithoutFastRefresh) {
            val version = options.version
            if (options.isShowTapHelp || isVersionEquals("0.0.0", version)) {
                try {
                    val prefs = options.prefs
                    val edit = prefs.edit()
                    edit.putBoolean(GlobalOptions.DRAW_OFF_PAGE, false)
                    edit.putString(GlobalOptions.VERSION, VERSION_NAME)
                    edit.commit()
                } catch (e: Exception) {
                    log(e)
                }

            }
        }
    }

    fun setLangCode(langCode: String) {
        this.langCode = langCode
        updateLanguage(resources)
    }

    fun updateLanguage(res: Resources) {
        try {
            val defaultLocale = Locale.getDefault()
            log("Updating locale to $langCode from ${defaultLocale.language}")
            val dm = res.displayMetrics
            val conf = res.configuration
            conf.locale =
                if (langCode == null || "DEFAULT" == langCode) defaultLocale else Locale(langCode)
            res.updateConfiguration(conf, dm)
        } catch (e: Exception) {
            log("Error setting locale: " + langCode!!, e)
        }

    }

    fun onNewBook(fileName: String) {
        tempOptions = TemporaryOptions().also { it.openedFile = fileName }
    }


    fun applyTheme(activity: Activity) {
        val themeId = themeId

        if (themeId != -1) {
            activity.setTheme(themeId)
        }
    }

    fun getBookmarkAccessor(): BookmarkAccessor {
        if (bookmarkAccessor == null) {
            bookmarkAccessor = BookmarkAccessor(this)
        }
        return bookmarkAccessor!!
    }

    fun destroyDb() {
        if (bookmarkAccessor != null) {
            bookmarkAccessor!!.close()
            bookmarkAccessor = null
        }
    }

    //temporary hack
    fun processBookOptionChange(key: String, value: Any) {
        viewActivity?.controller?.run {
            when (key) {
                "walkOrder" -> changetWalkOrder(value as String)
                "pageLayout" -> changetPageLayout(value as Int)
                "contrast" -> changeContrast(value as Int)
                "threshold" -> changeThreshhold(value as Int)
                "screenOrientation" -> changeOrinatation(value as String)
                "colorMode" -> changeColorMode(value as String, true)
                "zoom" -> changeZoom(value as Int)
            }
        }
    }

    companion object {
        var instance: OrionApplication by Delegates.notNull()
            private set

        fun logOrionAndDeviceInfo() {
            log("Orion Viewer $VERSION_NAME")
            log("Device: $DEVICE")
            log("Model: $MODEL")
            log("Manufacturer:  $MANUFACTURER")
            log("Android version :  $CODENAME $RELEASE")
        }

        @JvmStatic
        fun createDevice(): AndroidDevice {
            if (ONYX_DEVICE) {
                log("Using Onyx Device")
                return OnyxDevice()
            }

            if (RK30SDK) {
                log("Using RK30SDK")
                return MagicBookBoeyeDevice()
            }


            log("Using default android device")
            return AndroidDevice()
        }

        @JvmField
        val MANUFACTURER = getField("MANUFACTURER")

        @JvmField
        val MODEL = getField("MODEL")

        @JvmField
        val DEVICE = getField("DEVICE")

        @JvmField
        val HARDWARE = getField("HARDWARE")

        @JvmField
        val ONYX_DEVICE = "ONYX".equals(MANUFACTURER, ignoreCase = true) && OnyxUtil.isEinkDevice

        @JvmField
        val RK30SDK = "rk30sdk".equals(MODEL, ignoreCase = true) && ("T62D".equals(
            DEVICE,
            ignoreCase = true
        ) || DEVICE.lowercase(Locale.getDefault()).contains("onyx"))

        private fun getField(name: String): String =
            try {
                Build::class.java.getField(name).get(null) as String
            } catch (e: Exception) {
                log("Exception on extracting Build property: $name")
                "!ERROR!"
            }


        @JvmField
        val version: String = Build.VERSION.INCREMENTAL
    }
}
