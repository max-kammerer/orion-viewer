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

package universe.constellation.orion.viewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import universe.constellation.orion.viewer.device.AndroidDevice
import universe.constellation.orion.viewer.device.Device
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication

abstract class OrionBaseActivity(createDevice: Boolean = true, val viewerType: Int = Device.DEFAULT_ACTIVITY) : AppCompatActivity() {

    val device: AndroidDevice? = if (createDevice) OrionApplication.createDevice() else null

    protected var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    lateinit var toolbar: Toolbar
        private set

    open val view: OrionScene?
        get() = null

    val orionContext: OrionApplication
        get() = applicationContext as OrionApplication

    val applicationDefaultOrientation: String
        get() = orionContext.options.getStringProperty(GlobalOptions.SCREEN_ORIENTATION, "DEFAULT")

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, -1)
    }

    protected fun onOrionCreate(savedInstanceState: Bundle?, layoutId: Int, addToolbar: Boolean = true) {
        orionContext.applyTheme(this)
        orionContext.updateLanguage(resources)

        if (this is OrionViewerActivity || this is OrionFileManagerActivity) {
            val screenOrientation = getScreenOrientation(applicationDefaultOrientation)
            changeOrientation(screenOrientation)
        }

        super.onCreate(savedInstanceState)

        device?.onCreate(this)

        if (layoutId != -1) {
            setContentView(layoutId)
            toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            if (addToolbar) {
                setSupportActionBar(toolbar)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        device?.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            device?.onWindowGainFocus()
        }
    }

    override fun onPause() {
        super.onPause()
        device?.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        device?.onUserInteraction()
    }

    protected open fun findMyViewById(id: Int): View {
        return findViewById(id)
    }

    protected open fun onAnimatorCancel() {

    }

    protected open fun onApplyAction() {

    }

    fun showWarning(warning: String) {
        Toast.makeText(this, warning, Toast.LENGTH_SHORT).show()
    }

    fun showWarning(stringId: Int) {
        showWarning(resources.getString(stringId))
    }

    fun showFastMessage(stringId: Int) {
        showWarning(resources.getString(stringId))
    }

    fun showLongMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun showFastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun changeOrientation(orientationId: Int) {
        println("Display orientation " + requestedOrientation + " screenOrientation " + window.attributes.screenOrientation)
        if (requestedOrientation != orientationId) {
            requestedOrientation = orientationId
        }
    }

    fun getScreenOrientation(id: String): Int {
        return when (id) {
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "LANDSCAPE_INVERSE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "PORTRAIT_INVERSE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun getScreenOrientationItemPos(id: String): Int {
        return when (id) {
            "LANDSCAPE" -> 2
            "PORTRAIT" -> 1
            "LANDSCAPE_INVERSE" -> 4
            "PORTRAIT_INVERSE" -> 3
            else -> 0
        }
    }

    fun showAlert(title: String, message: String) {
        val builder = createThemedAlertBuilder()
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }

        builder.create().show()
    }

    fun showAlert(titleId: Int, messageId: Int) {
        val builder = createThemedAlertBuilder()
        builder.setTitle(titleId)
        builder.setMessage(messageId)

        builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }

        builder.create().show()
    }

    fun createThemedAlertBuilder(): AlertDialog.Builder {
        return AlertDialog.Builder(this)
    }

    protected fun doTrack(keyCode: Int): Boolean {
        return keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_BACK
    }

}