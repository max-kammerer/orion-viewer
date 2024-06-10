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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.color.MaterialColors
import universe.constellation.orion.viewer.device.AndroidDevice
import universe.constellation.orion.viewer.device.Device
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication

abstract class OrionBaseActivity(val viewerType: Int = Device.DEFAULT_ACTIVITY) : AppCompatActivity() {

    val device: AndroidDevice? = if (viewerType == Device.VIEWER_ACTIVITY) OrionApplication.createDevice() else null

    lateinit var toolbar: Toolbar
        private set

    val orionApplication: OrionApplication
        get() = applicationContext as OrionApplication

    val applicationDefaultOrientation: String
        get() = orionApplication.options.getStringProperty(GlobalOptions.SCREEN_ORIENTATION, "DEFAULT")

    val analytics
        get() = orionApplication.analytics

    val globalOptions
        get() = orionApplication.options

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, -1)
    }

    @JvmOverloads
    protected fun onOrionCreate(savedInstanceState: Bundle?, layoutId: Int, addToolbar: Boolean = true, displayHomeAsUpEnabled: Boolean = false) {
        orionApplication.applyTheme(this)
        orionApplication.updateLanguage(resources)

        if (this is OrionViewerActivity || this is OrionFileManagerActivityBase) {
            val screenOrientation = getScreenOrientation(applicationDefaultOrientation)
            changeOrientation(screenOrientation)
        }

        super.onCreate(savedInstanceState)

        device?.onCreate(this)

        if (layoutId != -1) {
            setContentView(layoutId)
            if (addToolbar) {
                toolbar = findViewById<View>(R.id.toolbar) as Toolbar
                setSupportActionBar(toolbar)
                if (displayHomeAsUpEnabled) {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                }
                val tintColor = MaterialColors.getColor(toolbar, R.attr.navIconTint)
                toolbar.getOverflowIcon()?.apply {
                    DrawableCompat.setTint(this, tintColor)
                }
                toolbar.navigationIcon?.apply {
                    DrawableCompat.setTint(this, tintColor)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
        log("Display orientation: lastRequested=" + requestedOrientation + " screenOrientation=" + window.attributes.screenOrientation + " newOrientation=$orientationId")
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

    protected fun checkPermissionGranted(grantResults: IntArray, permissions: Array<String>, checkPermission: String): Boolean {
        return grantResults.zip(permissions).any { (grantResult, permission) ->
            checkPermission == permission && grantResult == PackageManager.PERMISSION_GRANTED
        }
    }

    internal fun openHelpActivity(itemId: Int) {
        val intent = Intent()
        intent.setClass(this, OrionHelpActivity::class.java)
        intent.putExtra(OrionHelpActivity.OPEN_ABOUT_TAB, itemId == R.id.about_menu_item)
        startActivity(intent)
    }
}

fun Activity.getVectorDrawable(id: Int, color: Int = 0): Drawable {
    val drawable = VectorDrawableCompat.create(resources, id, this.theme)
        ?: ColorDrawable(resources.getColor(R.color.orion_orange))
//    if (color != 0) {
//        DrawableCompat.setTint(drawable, resources.getColor(R.color.orion_orange))
//        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
//    }
    return drawable
}

fun Context.dpToPixels(value: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    ).toInt()
}