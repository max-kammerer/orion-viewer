package universe.constellation.orion.viewer

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.*
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import java.io.File


open class SaveNotification : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val uri = arguments!!.getParcelable<Uri>(URI)!!
        val mimeType = arguments!!.getString(TYPE)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.please_save_file))
                .setItems(R.array.save_options) { _, which ->
                    val myActivity = activity as OrionViewerActivity
                    when(which) {
                        0 -> {
                            myActivity.startActivity(
                                Intent(myActivity, OrionSaveFileActivity::class.java).apply {
                                    putExtra(URI, uri)
                                }
                            )
                        }
                        1 -> {
                            val toFile = createTmpFile(
                                activity!!,
                                getExtension(uri, mimeType)
                            )

                            saveFileAndDoAction(myActivity, uri, toFile) {
                                myActivity.openFileAndDestroyOldController(it.path)
                            }
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
            toFile.parentFile.mkdirs()
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

        fun saveFileAndDoAction(
                myActivity: Activity,
                uri: Uri,
                toFile: File,
                action: (File) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                val progressBar = ProgressDialog(myActivity)
                progressBar.isIndeterminate = true
                progressBar.show()
                try {
                    withContext(Dispatchers.Default) {
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

