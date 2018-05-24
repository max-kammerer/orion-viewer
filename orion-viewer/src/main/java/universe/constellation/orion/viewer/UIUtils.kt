package universe.constellation.orion.viewer

import android.content.Context
import android.widget.Toast
import androidx.core.widget.toast


fun Context.showError(error: String, ex: Exception) {
    toast(error + ": " + ex.message, Toast.LENGTH_LONG).show()
    log(ex)
}