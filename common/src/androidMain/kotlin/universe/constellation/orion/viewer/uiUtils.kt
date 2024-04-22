package universe.constellation.orion.viewer

import android.app.Activity
import android.widget.Toast

fun showAndLogError(orionBaseActivity: Activity, error: String, ex: Exception) {
    Toast.makeText(orionBaseActivity, error + ": " + ex.message, Toast.LENGTH_LONG).show()
    log(ex)
}

fun showAndLogError(orionBaseActivity: Activity, e: Exception) {
    showAndLogError(orionBaseActivity, "Error", e)
}