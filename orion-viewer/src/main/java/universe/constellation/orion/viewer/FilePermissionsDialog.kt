package universe.constellation.orion.viewer

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.widget.ListView
import universe.constellation.orion.viewer.IntentFallbackDialog.Companion.createTmpFile
import universe.constellation.orion.viewer.IntentFallbackDialog.Companion.getExtension
import universe.constellation.orion.viewer.IntentFallbackDialog.Companion.saveFileAndDoAction
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity


open class FilePermissionsDialog {
     fun showReadPermissionDialog(activity: Activity, intent: Intent, hasFullAccess: Boolean): Dialog {
         val uri = intent.data!!
         val mimeType = intent.type
         val builder = AlertDialog.Builder(activity)
         val view = activity.layoutInflater.inflate(R.layout.permissions_problem_dialog, null)
         builder.setTitle(R.string.permission_file_read_dialog_title).setView(view)
                 .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                     dialog.cancel()
                 }.setMessage(R.string.permission_read_file_warning)
         val alertDialog = builder.create()
         val fallbacks = view.findViewById<ListView>(R.id.permissions_fallback_list)
         fallbacks.setOnItemClickListener { _, _, position, _ ->
             val myActivity = activity as OrionViewerActivity
             when (position) {
                 0 -> {
                     activity.checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_BOOK_OPEN)
                     alertDialog.dismiss()
                 }
                 1 -> {
                     saveInTmpFile(uri, mimeType, myActivity, alertDialog, intent, activity)
                 }
                 else -> error("Unknown save option: $position")
             }
         }
         return alertDialog
    }


}

internal fun saveInTmpFile(
    uri: Uri,
    mimeType: String?,
    myActivity: OrionViewerActivity,
    alertDialog: AlertDialog,
    intent: Intent,
    activity: Activity
) {
    val extension = getExtension(uri, mimeType, myActivity.contentResolver)
    if (extension == null) {
        alertDialog.dismiss()
        myActivity.showErrorReportDialog(
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            intent.toString()
        )
        return
    }

    val toFile = createTmpFile(
        activity,
        extension
    )

    saveFileAndDoAction(myActivity, uri, toFile) {
        alertDialog.dismiss()
        myActivity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(myActivity.applicationContext, OrionViewerActivity::class.java)
                data = Uri.fromFile(toFile)
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("from_intent_fallback", true)
            }
        )
    }
}

