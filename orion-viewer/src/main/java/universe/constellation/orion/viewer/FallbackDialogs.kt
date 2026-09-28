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
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Locale

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

    /**
     * The provider behind the uri refuses to open it (the file is gone or the grant has expired),
     * so copying it anywhere is hopeless and offering the copy would only end in a crash report.
     */
    fun createUnreadableSourceFallbackDialog(activity: OrionViewerActivity, intent: Intent, error: Exception): Dialog {
        return createFallbackDialog(
            activity,
            null,
            intent,
            R.string.fileopen_source_unavailable,
            activity.getString(R.string.fileopen_source_unavailable_info, intent.data?.authority, error.describe()),
            null,
            listOfNotNull(
                R.string.fileopen_permissions_grant_read.takeIf { error is SecurityException && !hasReadStoragePermission(activity) },
                R.string.fileopen_open_recent_files
            ),
            error
        )
    }

    private fun createFallbackDialog(activity: OrionViewerActivity, fileInfo: FileInfo?, intent: Intent, title: Int, info: Int, defaultAction: Int?, list: List<Int>): Dialog {
        return createFallbackDialog(activity, fileInfo, intent, title, activity.getString(info), defaultAction, list)
    }

     //content intent
     private fun createFallbackDialog(activity: OrionViewerActivity, fileInfo: FileInfo?, intent: Intent, title: Int, info: String, defaultAction: Int?, list: List<Int>, exception: Exception? = null): Dialog {
         val dialogTitle = activity.getString(title)
         activity.showErrorOnFallbackPanel(dialogTitle, intent, cause = dialogTitle.takeIf { exception == null }, exception = exception)

         activity.analytics.dialog(FALLBACK_DIALOG, true)

         val uri = intent.data!!

         val view = activity.layoutInflater.inflate(R.layout.intent_problem_dialog, null)
         val infoText = view.findViewById<TextView>(R.id.intent_problem_info)
         infoText.text = info

         val builder = AlertDialog.Builder(activity)

         builder.setTitle(dialogTitle).setView(view)
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

        fun OrionBaseActivity.saveFileByUri(
            intent: Intent,
            originalContentUri: Uri,
            targetFileUri: Uri,
            handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
                exception.printStackTrace()
                if (exception is SourceUnavailableException) {
                    //nothing to report: the other app has withdrawn the file
                    analytics.logWarning(exception.message!!)
                    createThemedAlertBuilder()
                        .setTitle(R.string.fileopen_source_unavailable)
                        .setMessage(exception.message)
                        .setPositiveButton(R.string.string_close) { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    this@saveFileByUri.showErrorReportDialog(
                        R.string.error_on_file_saving_title,
                        R.string.error_on_file_saving_title,
                        intent,
                        "source=$originalContentUri\ntargetFile=$targetFileUri",
                        exception
                    )
                }
            },
            callbackAction: () -> Unit
        ) {
            val res = this.orionApplication.idlingRes
            res.busy()

            GlobalScope.launch(Dispatchers.Main + handler) {
                val progressBar = ProgressDialog(this@saveFileByUri)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    withContext(Dispatchers.IO) {
                        (openSource(originalContentUri)?.use { input ->
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

        /**
         * One-off uri grants die with the activity they were given to and providers drop files at will,
         * so the source can turn unreadable between the dialog and the click that starts the copy.
         */
        private fun OrionBaseActivity.openSource(uri: Uri): InputStream? {
            return try {
                contentResolver.openInputStream(uri)
            } catch (e: FileNotFoundException) {
                throw SourceUnavailableException(this, uri, e)
            } catch (e: SecurityException) {
                throw SourceUnavailableException(this, uri, e)
            }
        }
    }
}

class SourceUnavailableException(context: Context, uri: Uri, cause: Exception) : IOException(
    context.getString(R.string.fileopen_source_unavailable_message, uri.authority, cause.describe()),
    cause
)

/** Provider exceptions often carry no message, so at least name the class. */
internal fun Exception.describe(): String {
    val message = message
    return if (message.isNullOrBlank()) javaClass.simpleName else "${javaClass.simpleName}: $message"
}

private fun Context.tmpContentFolderForFile(fileInfo: FileInfo?): File {
    val contentFolder = cacheContentFolder()
    return if (fileInfo == null) contentFolder
    /* The id is the last uri segment: for a document it's like "primary:Download/<title>.pdf". */
    else File(contentFolder, fileInfo.uri.host + "/" + fitFileName(fileInfo.id ?: ("_" + fileInfo.size)) + "/")
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
            intent
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
        return File(fileFolder, fitFileName(fileInfo.name!!))
    } else {
        val fullName = (fileInfo?.name ?: fileInfo?.file?.name ?: "test_book")
        val noExtName = if (fullName.lowercase(Locale.getDefault()).endsWith(".$extension")) {
            fullName.substringBeforeLast(".$extension")
        } else {
            fullName
        }

        /* createTempFile appends up to 19 random digits before the suffix. */
        val prefix = fitFileName(noExtName, MAX_FILE_NAME_BYTES - ".$extension".utf8Size() - 19)
        return File.createTempFile(
            if (prefix.length < 3) "tmp$prefix" else prefix,
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
    val file = File(tmpContentFolderForFile(fileInfo), fitFileName(fileInfo.name ?: return null))
    return file.takeIf { it.exists() }
}

