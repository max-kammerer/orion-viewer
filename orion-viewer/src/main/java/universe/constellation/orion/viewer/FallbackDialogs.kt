package universe.constellation.orion.viewer

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.FallbackDialogs.Companion.saveFileByUri
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.analytics.FALLBACK_DIALOG
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.android.isContentScheme
import universe.constellation.orion.viewer.android.isContentUri
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.formats.FileFormats.Companion.getFileExtension
import java.io.File

class ResourceIdAndString(val id: Int, val value: String) {
    override fun toString(): String {
        return value
    }
}

open class FallbackDialogs {

    fun createBadIntentFallbackDialog(activity: OrionViewerActivity, fileInfo: FileInfo?, intent: Intent): Dialog {
        val isContentScheme = intent.isContentScheme()
        return createFallbackDialog(
            activity,
            fileInfo,
            intent,
            R.string.fileopen_error_during_intent_processing,
            R.string.fileopen_error_during_intent_processing_info,
            if (isContentScheme) R.string.fileopen_open_in_temporary_file else null,
            listOfNotNull(
                R.string.fileopen_permissions_grant_read.takeIf { !hasReadStoragePermission(activity) },
                R.string.fileopen_open_in_temporary_file.takeIf { isContentScheme },
                R.string.fileopen_save_to_file.takeIf {isContentScheme && hasReadStoragePermission(activity)},
                R.string.fileopen_open_recent_files,
                R.string.fileopen_report_error_by_github_and_return,
                R.string.fileopen_report_error_by_email_and_return
            )
        )
    }

    fun createPrivateResourceFallbackDialog(activity: OrionViewerActivity, fileInfo: FileInfo, intent: Intent): Dialog {
        val isContentScheme = intent.isContentScheme()
        return createFallbackDialog(
            activity,
            fileInfo,
            intent,
            R.string.fileopen_private_resource_access,
            R.string.fileopen_private_resource_access_info,
            if (isContentScheme) R.string.fileopen_open_in_temporary_file else null,
            listOfNotNull(
                R.string.fileopen_permissions_grant_read.takeIf { !isContentScheme && !hasReadStoragePermission(activity)},
                R.string.fileopen_open_in_temporary_file.takeIf { isContentScheme },
                R.string.fileopen_save_to_file.takeIf {isContentScheme && hasReadStoragePermission(activity)},
                R.string.fileopen_open_recent_files.takeIf { isContentScheme },
                R.string.fileopen_report_error_by_github_and_return.takeIf { !isContentScheme },
                R.string.fileopen_report_error_by_email_and_return.takeIf { !isContentScheme }
            )
        )
    }

    fun createGrantReadPermissionsDialog(activity: OrionViewerActivity, fileInfo: FileInfo, intent: Intent): Dialog {
        val isContentScheme = intent.isContentScheme()
        return createFallbackDialog(
            activity,
            fileInfo,
            intent,
            R.string.fileopen_permission_dialog,
            R.string.fileopen_permission_dialog_info,
            R.string.fileopen_permissions_grant_read,
            listOfNotNull(
                R.string.fileopen_permissions_grant_read,
                R.string.fileopen_open_in_temporary_file.takeIf { isContentScheme },
                R.string.fileopen_open_recent_files
            )
        )
    }

     //content intent
     private fun createFallbackDialog(activity: OrionViewerActivity, fileInfo: FileInfo?, intent: Intent, title: Int, info: Int, defaultAction: Int?, list: List<Int>): Dialog {
         activity.analytics.dialog(FALLBACK_DIALOG, true)

         val uri = intent.data!!

         val view = activity.layoutInflater.inflate(R.layout.intent_problem_dialog, null)
         val infoText = view.findViewById<TextView>(R.id.intent_problem_info)
         infoText.setText(info)

         val builder = AlertDialog.Builder(activity)
         builder.setTitle(activity.getString(title)).setView(view)
             .setNegativeButton(R.string.string_cancel) { dialog, _ ->
                 dialog.cancel()
             }

         if (defaultAction != null) {
             builder.setPositiveButton(defaultAction) { dialog, _ ->
                 processAction(defaultAction, activity, fileInfo, dialog, uri, intent)
             }
         }

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
             builder.setOnDismissListener { _ ->
                 activity.analytics.dialog(FALLBACK_DIALOG, false)
             }
         }

         val alertDialog = builder.create()

         val fallbacks = view.findViewById<ListView>(R.id.intent_fallback_list)
         fallbacks.adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, list.map { ResourceIdAndString(it, activity.getString(it)) })

         fallbacks.setOnItemClickListener { _, _, position, _ ->
             val id = (fallbacks.adapter.getItem(position) as ResourceIdAndString).id
             processAction(id, activity, fileInfo, alertDialog, uri, intent)
         }
         return alertDialog
    }

    private fun processAction(
        id: Int,
        activity: OrionViewerActivity,
        fileInfo: FileInfo?,
        alertDialog: DialogInterface,
        uri: Uri,
        intent: Intent
    ) {
        when (id) {
            R.string.fileopen_permissions_grant_read -> {
                activity.checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_BOOK_OPEN)
                alertDialog.dismiss()
            }

            R.string.fileopen_save_to_file -> {
                if (isAtLeastKitkat()) {
                    sendCreateFileRequest(activity, fileInfo, intent)
                } else {
                    activity.startActivity(
                        Intent(activity, OrionSaveFileActivity::class.java).apply {
                            putExtra(URI, uri)
                            fileInfo?.name?.let {
                                putExtra(OrionSaveFileActivity.SUGGESTED_FILE_NAME, it)
                            }
                        }
                    )
                }
                alertDialog.dismiss()
            }

            R.string.fileopen_open_in_temporary_file -> {
                saveContentInTmpFile(uri, activity, alertDialog, intent, activity, fileInfo)
            }

            R.string.fileopen_open_recent_files -> {
                alertDialog.dismiss()
                activity.startActivity(
                    Intent(activity, OrionFileManagerActivity::class.java).apply {
                        putExtra(OrionFileManagerActivity.OPEN_RECENTS_TAB, true)

                    })
            }

            R.string.fileopen_report_error_by_github_and_return -> {
                val title =
                    activity.applicationContext.getString(R.string.crash_on_intent_opening_title)
                activity.reportErrorVia(false, title, intent.toString())

            }

            R.string.fileopen_report_error_by_email_and_return -> {
                val title =
                    activity.applicationContext.getString(R.string.crash_on_intent_opening_title)
                activity.reportErrorVia(true, title, intent.toString())
            }

            else -> error("Unknown option id: $id")
        }
    }

    companion object {

        const val URI = "URI"

        fun Activity.saveFileByUri(
            intent: Intent?,
            originalContentUri: Uri,
            targetFileUri: Uri,
            callbackAction: () -> Unit
        ) {
            val res = (this as OrionBaseActivity).orionContext.idlingRes
            res.busy()
            val handler = CoroutineExceptionHandler { _, exception ->
                exception.printStackTrace()
                showErrorReportDialog(
                    R.string.error_on_file_saving_title,
                    R.string.error_on_file_saving_title,
                    "intent=" + intent?.toString() + "\ntargetFile=$targetFileUri",
                    exception
                )
            }

            GlobalScope.launch(Dispatchers.Main + handler) {
                val progressBar = ProgressDialog(this@saveFileByUri)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    withContext(Dispatchers.IO) {
                        (contentResolver.openInputStream(originalContentUri)?.use { input ->
                            contentResolver.openOutputStream(targetFileUri)?.use { output ->
                                input.copyTo(output)
                            } ?: error("Can't open output stream for $targetFileUri")
                        } ?: error("Can't read file data: $originalContentUri"))
                    }
                    callbackAction()
                } finally {
                    progressBar.dismiss()
                    res.free()
                }
            }
        }
    }
}

private fun Context.tmpContentFolderForFile(fileInfo: FileInfo?): File {
    val contentFolder = cacheContentFolder()
    return if (fileInfo == null) contentFolder
    else File(contentFolder, fileInfo.uri.host + "/" + (fileInfo.id ?: ("_" + fileInfo.size)) + "/")
}

fun Context.cacheContentFolder(): File {
    return File(cacheDir, ContentResolver.SCHEME_CONTENT)
}

private fun saveContentInTmpFile(
    uri: Uri,
    myActivity: OrionViewerActivity,
    dialog: DialogInterface,
    intent: Intent,
    activity: Activity,
    fileInfo: FileInfo?
) {
    val extension = myActivity.contentResolver.getFileExtension(intent)
    if (extension == null) {
        dialog.dismiss()
        //TODO proper message
        myActivity.showErrorReportDialog(
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            intent.toString()
        )
        return
    }

    val toFile = activity.createTmpFile(fileInfo, extension)

    myActivity.saveFileByUri(intent, uri, toFile.toUri()) {
        dialog.dismiss()
        myActivity.onNewIntentInternal(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(myActivity.applicationContext, OrionViewerActivity::class.java)
                data = Uri.fromFile(toFile)
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(OrionViewerActivity.USER_INTENT, false)
            }
        )
    }
}

internal fun Context.createTmpFile(fileInfo: FileInfo?, extension: String): File {
    val fileFolder = tmpContentFolderForFile(fileInfo)
    fileFolder.mkdirs()
    if (fileInfo?.canHasTmpFileWithStablePath() == true) {
        return File(fileFolder, fileInfo.name!!)
    } else {
        val fileName = (fileInfo?.name ?: fileInfo?.path?.substringAfterLast("/") ?: "test_bool").substringBeforeLast(".")
        return File.createTempFile(
            if (fileName.length < 3) "tmp$fileName" else fileName,
            ".$extension",
            fileFolder
        )
    }
}



@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun sendCreateFileRequest(activity: Activity, fileInfo: FileInfo?, readIntent: Intent) {
    val createFileIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    createFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
    val mimeType = readIntent.type ?: readIntent.data?.let {
        activity.contentResolver.getType(it)
    }
    if (mimeType != null) {
        createFileIntent.type = mimeType
    }
    fileInfo?.name?.let {
        createFileIntent.putExtra(Intent.EXTRA_TITLE, it)
    }
    activity.startActivityForResult(createFileIntent, OrionViewerActivity.SAVE_FILE_RESULT)
}

fun FileInfo.canHasTmpFileWithStablePath(): Boolean {
    return !id.isNullOrBlank() && size != 0L && !name.isNullOrBlank() && uri.isContentUri
}

fun Context.getStableTmpFileIfExists(fileInfo: FileInfo): File? {
    if (!fileInfo.canHasTmpFileWithStablePath()) return null
    val file = File(tmpContentFolderForFile(fileInfo), fileInfo.name ?: return null)
    return file.takeIf { it.exists() }
}

