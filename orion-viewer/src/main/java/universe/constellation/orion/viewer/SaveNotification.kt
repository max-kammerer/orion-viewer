package universe.constellation.orion.viewer

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import kotlinx.coroutines.*
import universe.constellation.orion.viewer.OrionViewerActivity.Companion.SAVE_FILE_RESULT
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import java.io.File


open class SaveNotification : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val uri = Uri.parse(arguments!!.getString(URI)!!)
        val mimeType = arguments!!.getString(TYPE)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Please save file before opening...")
                .setItems(R.array.save_options) { _, which ->
                    val myActivity = activity as OrionViewerActivity
                    when(which) {
                        0 -> {
                            myActivity.startActivityForResult(
                                Intent(myActivity, OrionSaveFileActivity::class.java).apply {
                                },
                                SAVE_FILE_RESULT
                            )
                        }
                        1 -> {
                            val toFile = createTmpFile(
                                activity!!,
                                getExtension(uri, mimeType)
                            )

                            saveFileAndOpen(myActivity, uri, toFile)
                        }
                        2 -> myActivity.startActivity(
                            Intent(myActivity, OrionFileManagerActivity::class.java).apply {
                                putExtra(OrionFileManagerActivity.OPEN_RECENTS_TAB, true)
                            }
                        )
                        else -> error("Unknown save option: $which")
                    }
                }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
        return builder.create()
    }

    companion object {

        const val URI = "URI"
        const val EXTENSION = "EXTENSION"
        const val TYPE = "TYPE"

        private fun saveFileInto(context: Context, uri: Uri, toFile: File): File {
            toFile.mkdirs()
            toFile.createNewFile()

            val input = context.contentResolver.openInputStream(uri)

            input.use {
                toFile.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            return toFile
        }

        private fun createTmpFile(context: Context, extension: String) =
            File.createTempFile("book", ".$extension", context.cacheDir)!!

        fun getExtension(uri: Uri, mimeType: String?): String {
            return uri.path.substringAfterLast('.').takeIf {
                FileChooserAdapter.supportedExtensions.contains(it)
            } ?: mimeType?.let { if (it.endsWith("djvu")) "djvu" else "pdf" } ?: error("Unknown extension $uri")
        }

        fun saveFileAndOpen(
            myActivity: OrionViewerActivity,
            uri: Uri,
            toFile: File
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                val progressBar = ProgressDialog(myActivity)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    val newFile = withContext(Dispatchers.Default) {
                        saveFileInto(
                                myActivity,
                                uri,
                                toFile.also { log("Saving file into $it") }
                        )
                    }
                    myActivity.openFileAndDestroyOldController(newFile.path)
                } finally {
                    progressBar.dismiss()
                }
            }
        }

    }
}

