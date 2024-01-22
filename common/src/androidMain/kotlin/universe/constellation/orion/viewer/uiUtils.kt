package universe.constellation.orion.viewer

import android.app.Activity
import android.widget.Toast

fun showError(orionBaseActivity: Activity, error: String, ex: Exception) {
    Toast.makeText(orionBaseActivity, error + ": " + ex.message, Toast.LENGTH_LONG).show()
    log(ex)
}

fun showError(orionBaseActivity: Activity, e: Exception) {
    showError(orionBaseActivity, "Error", e)
}