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
import android.os.Environment
import android.system.Os
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.multidex.MultiDex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import universe.constellation.orion.viewer.AndroidLogger
import universe.constellation.orion.viewer.AndroidLogger.startLogger
import universe.constellation.orion.viewer.AndroidLogger.stopLogger
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.BuildConfig.DEBUG
import universe.constellation.orion.viewer.BuildConfig.VERSION_NAME
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.analytics.Analytics
import universe.constellation.orion.viewer.bookmarks.BookmarkAccessor
import universe.constellation.orion.viewer.device.AndroidDevice
import universe.constellation.orion.viewer.device.MagicBookBoeyeDevice
import universe.constellation.orion.viewer.device.OnyxDevice
import universe.constellation.orion.viewer.device.OnyxUtil
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.logger
import universe.constellation.orion.viewer.prefs.GlobalOptions.DEFAULT_LANGUAGE
import universe.constellation.orion.viewer.test.IdlingResource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates

class OrionApplication : Application(), DefaultLifecycleObserver {

    internal var idlingRes = IdlingResource()

    val options: GlobalOptions by lazy {
        GlobalOptions(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this), true)
    }

    val keyBinding: GlobalOptions by lazy {
        GlobalOptions(this, getSharedPreferences("key_binding", Context.MODE_PRIVATE), false)
    }

    val analytics: Analytics by lazy {
        Analytics.initialize(
            contentResolver,
            BuildConfig.ANALYTICS
        )
    }

    var tempOptions: TemporaryOptions? = null
        private set

    private var bookmarkAccessor: BookmarkAccessor? = null

    var viewActivity: OrionViewerActivity? = null

    var currentBookParameters: LastPageInfo? = null

    val device = createDevice()

    private var currentLanguage: String = DEFAULT_LANGUAGE

    private val appTheme: String
        get() {
            val theme = options.applicationTheme
            return if ("DEFAULT" == theme) device.defaultTheme else theme
        }

    private val themeId: Int
        get() = when(appTheme) {
            "DARK" -> R.style.Theme_Orion_Dark_NoActionBar
            "LIGHT" -> R.style.Theme_Orion_Light_NoActionBar
            "ANDROID_LIGHT" ->  R.style.Theme_Orion_Android_Light_NoActionBar
            "ANDROID_DARK" ->  R.style.Theme_Orion_Android_Dark_NoActionBar
            else -> R.style.Theme_Orion_Dark_NoActionBar
        }

    val sdkVersion: Int
        get() = Build.VERSION.SDK_INT

    override fun onCreate() {
        logger = AndroidLogger
        startOrStopDebugLogger(options.getBooleanProperty("DEBUG", false))
        instance = this
        super<Application>.onCreate()
        setLanguage(options.appLanguage)
        logOrionAndDeviceInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val fileToCopy = File(filesDir, "djvuConf")
            val envPath = File(fileToCopy, "osi").absolutePath
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                copyResIfNotExists(assets, "osi", fileToCopy)
            }
            Os.setenv("DJVU_CONFIG_DIR", envPath, true)
        }
    }

    fun setLanguage(langCode: String) {
        currentLanguage = langCode
    }

    fun updateLanguage(res: Resources) {
        try {
            val currentLocales = ConfigurationCompat.getLocales(res.configuration)
            val newLocale =
                if (DEFAULT_LANGUAGE == currentLanguage) Locale.getDefault() else Locale(
                    currentLanguage
                )
            if (!currentLocales.isEmpty) {
                if (newLocale.language == currentLocales[0]?.language) return
            }
            log("Updating locale to $currentLanguage from ${currentLocales[0]?.language}")

            if (Build.VERSION.SDK_INT >= 17) {
                val prevLocales = Array(currentLocales.size()) { currentLocales.get(it) }
                ConfigurationCompat.setLocales(
                    res.configuration,
                    LocaleListCompat.create(newLocale, *prevLocales)
                )
            } else {
                res.configuration.locale = newLocale
            }
            res.updateConfiguration(res.configuration, res.displayMetrics)
        } catch (e: Exception) {
            log("Error setting locale: $currentLanguage", e)
            analytics.error(e)
        }

    }

    fun onNewBook(fileName: String) {
        tempOptions = TemporaryOptions().also { it.openedFile = fileName }
    }


    fun applyTheme(activity: Activity) {
        if (this.themeId != -1) {
            activity.setTheme(this.themeId)
        }
    }

    fun getBookmarkAccessor(): BookmarkAccessor {
        if (bookmarkAccessor == null) {
            bookmarkAccessor = BookmarkAccessor(this)
        }
        return bookmarkAccessor!!
    }

    fun destroyMainActivity() {
        destroyDb()
        viewActivity = null
        currentBookParameters = null
    }

    private fun destroyDb() {
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

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (DEBUG) {
            MultiDex.install(this)
        }
    }

    fun startOrStopDebugLogger(start: Boolean) {
        if (start) {
            try {
                val download = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (download != null && download.canRead()) {
                    val logFolder = File(download, "OrionViewer")
                    logFolder.mkdirs()
                    val filePrefix = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_").format(Date())
                    val file = File.createTempFile(filePrefix, ".trace.txt", logFolder)
                    log("Starting Logger in $file")
                    startLogger(file)
                } else {
                    log("Can't start logger")
                }
            } catch (e: Throwable) {
                log(e)
            }
        } else {
            log("Stopping logger")
            stopLogger()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        startOrStopDebugLogger(false)
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
