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
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import java.io.File

class ResourceIdAndString(val id: Int, val value: String) {
    override fun toString(): String {
        return value
    }
}

open class FallbackDialogs {

    private fun Intent.isContentScheme(): Boolean {
        return ContentResolver.SCHEME_CONTENT.equals(data?.scheme, ignoreCase = true)
    }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
                saveInTmpFile(uri, activity, alertDialog, intent, activity, fileInfo)
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

        private fun saveFileInto(context: Context, uri: Uri, toFile: File): File {
            toFile.parentFile?.mkdirs()

            val input = context.contentResolver.openInputStream(uri) ?: error("Can't read file data: $uri")

            input.use {
                toFile.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            return toFile
        }

        internal fun createTmpFile(context: Context, fileName: String, extension: String) =
            File.createTempFile(
                if (fileName.length < 3) "tmp$fileName" else fileName,
                ".$extension",
                context.cacheDir
            )

        fun getExtension(uri: Uri, mimeType: String?, contentResolver: ContentResolver): String? {
            return uri.path?.substringAfterLast("/")?.substringAfterLast('.').takeIf {
                FileChooserAdapter.supportedExtensions.contains(it)
            } ?: mimeType?.findExtensionForMimeType() ?: uri.extractMimeTypeFromUri(contentResolver)?.run {
                findExtensionForMimeType() ?:
                MimeTypeMap.getSingleton().getExtensionFromMimeType(this).takeIf {  FileChooserAdapter.supportedExtensions.contains(it) }
            }
        }

        private fun Uri.extractMimeTypeFromUri(contentResolver: ContentResolver): String? {
            return contentResolver.getType(this)

        }

        private fun String?.findExtensionForMimeType(): String? {
            if (this  == null) return null
            return FileChooserAdapter.supportedExtensions.firstOrNull {
                it.contains(this)
            }
        }

        fun saveFileAndDoAction(
                myActivity: Activity,
                uri: Uri,
                toFile: File,
                action: (File) -> Unit
        ) {
            val handler = CoroutineExceptionHandler { _, exception ->
                exception.printStackTrace()
                AlertDialog.Builder(myActivity).setMessage(exception.message)
                    .setPositiveButton("OK"
                    ) { dialog, _ -> dialog.dismiss() }.create().show()
            }

            GlobalScope.launch(Dispatchers.Main + handler) {
                val progressBar = ProgressDialog(myActivity)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    withContext(Dispatchers.IO) {
                        saveFileInto(
                                myActivity,
                                uri,
                                toFile.also { log("Trying to save file into $it") }
                        )
                    }
                    action(toFile)
                } finally {
                    progressBar.dismiss()
                }
            }
        }

        fun saveFileIntoUri(
            myActivity: Activity,
            uri: Uri,
            toFile: Uri,
            action: (Uri) -> Unit
        ) {
            val handler = CoroutineExceptionHandler { _, exception ->
                exception.printStackTrace()
                AlertDialog.Builder(myActivity).setMessage(exception.message)
                    .setPositiveButton("OK"
                    ) { dialog, _ -> dialog.dismiss() }.create().show()
            }

            GlobalScope.launch(Dispatchers.Main + handler) {
                val progressBar = ProgressDialog(myActivity)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    withContext(Dispatchers.IO) {
                        val outputStream = myActivity.contentResolver.openOutputStream(toFile) ?: error("Can't open output stream for $toFile")
                        val input = myActivity.contentResolver.openInputStream(uri) ?: error("Can't read file data: $uri")

                        input.use {
                            outputStream.use { outputStream ->
                                input.copyTo(outputStream)
                            }
                        }
                    }
                    action(toFile)
                } finally {
                    progressBar.dismiss()
                }
            }
        }
    }
}

private fun saveInTmpFile(
    uri: Uri,
    myActivity: OrionViewerActivity,
    dialog: DialogInterface,
    intent: Intent,
    activity: Activity,
    fileInfo: FileInfo?
) {
    val extension = FallbackDialogs.getExtension(uri, intent.type, myActivity.contentResolver)
    if (extension == null) {
        dialog.dismiss()
        myActivity.showErrorReportDialog(
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
            intent.toString()
        )
        return
    }

    val toFile = FallbackDialogs.createTmpFile(
        activity,
        fileInfo?.name?.substringBeforeLast('.') ?: "temp_book",
        extension
    )

    FallbackDialogs.saveFileAndDoAction(myActivity, uri, toFile) {
        dialog.dismiss()
        myActivity.onNewIntentInternal(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(myActivity.applicationContext, OrionViewerActivity::class.java)
                data = Uri.fromFile(toFile)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
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
