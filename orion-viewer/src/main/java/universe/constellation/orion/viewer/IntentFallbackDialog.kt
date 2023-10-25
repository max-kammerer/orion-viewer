package universe.constellation.orion.viewer

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.ListView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import java.io.File


open class IntentFallbackDialog {

     //content intent
     fun showIntentFallbackDialog(activity: Activity, intent: Intent): Dialog {
         val uri = intent.data!!
         val mimeType = intent.type
         val builder = AlertDialog.Builder(activity)
         val view = activity.layoutInflater.inflate(R.layout.intent_problem_dialog, null)
         builder.setTitle(activity.applicationContext.getString(R.string.please_save_file)).setView(view)
                 .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                     dialog.cancel()
                 }
         val alertDialog = builder.create()
         val fallbacks = view.findViewById<ListView>(R.id.intent_fallback_list)
         fallbacks.setOnItemClickListener { _, _, position, _ ->
             val myActivity = activity as OrionViewerActivity
             when (position) {
                 0 -> {
                     myActivity.startActivity(
                             Intent(myActivity, OrionSaveFileActivity::class.java).apply {
                                 putExtra(URI, uri)
                             }
                     )
                     alertDialog.dismiss()
                 }
                 1 -> {
                     val extension = getExtension(uri, mimeType, myActivity.contentResolver)
                     if (extension == null) {
                         alertDialog.dismiss()
                         myActivity.showErrorReportDialog(
                                 myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
                                 myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title),
                                 intent.toString()
                         )
                         return@setOnItemClickListener
                     }

                     //should be granted automatically
                     Permissions.checkWritePermission(myActivity)
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
                 2 -> {
                     alertDialog.dismiss()
                     myActivity.startActivity(
                             Intent(myActivity, OrionFileManagerActivity::class.java).apply {
                                 putExtra(OrionFileManagerActivity.OPEN_RECENTS_TAB, true)

                             })
                 }
                 3 -> {
                     val title = myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title)
                     myActivity.reportErrorVia(false, title, intent.toString())

                 }
                 4 -> {
                     val title = myActivity.applicationContext.getString(R.string.crash_on_intent_opening_title)
                     myActivity.reportErrorVia(true, title, intent.toString())
                 }
                 else -> error("Unknown save option: $position")
             }
         }
         return alertDialog
    }

    companion object {

        const val URI = "URI"

        private fun saveFileInto(context: Context, uri: Uri, toFile: File): File {
            toFile.parentFile?.mkdirs()

            val input = context.contentResolver.openInputStream(uri) ?: error("Can't write to file: $uri")

            input.use {
                toFile.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            return toFile
        }

        internal fun createTmpFile(context: Context, extension: String) =
            File.createTempFile("temp_book", ".$extension", context.cacheDir)!!

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

    }
}

