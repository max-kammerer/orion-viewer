package universe.constellation.orion.viewer

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import kotlinx.coroutines.experimental.launch
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import java.io.File
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity


open class SaveNotification : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val uri = Uri.parse(arguments!!.getString(URI)!!)
        val mimeType = arguments!!.getString(TYPE)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Please save file before opening...")
                .setItems(R.array.save_options) { dialog, which ->
                    val myActivity = activity as OrionViewerActivity
                    when(which) {
                        0 -> {}
                        1 -> {
                            launch(UI) {
                                dialog.dismiss()

                                val progressBar = ProgressDialog(activity!!)
                                progressBar.isIndeterminate = true
                                progressBar.show()
                                try {
                                    val newFile = async(CommonPool) {
                                        saveFileInto(
                                            myActivity,
                                            uri,
                                            createTmpFile(
                                                activity!!,
                                                getExtension(uri, mimeType)
                                            ).also { log("Saving file into $it") })
                                    }.await()
                                    myActivity.openFileAndDestroyOldController(newFile.path)
                                } finally {
                                    progressBar.dismiss()
                                }
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

        fun saveFileInto(context: Context, uri: Uri, toFile: File): File {
            toFile.mkdirs()
            toFile.createNewFile()

            val input = context.contentResolver.openInputStream(uri)

            input.use {
                toFile.outputStream().use {
                    input.copyTo(it)
                }
            }
            return toFile
        }

        fun createTmpFile(context: Context, extension: String) =
            File.createTempFile("book", ".$extension", context.cacheDir)

        fun getExtension(uri: Uri, mimeType: String?): String {
            return uri.path.substringAfterLast('.').takeIf {
                FileChooserAdapter.supportedExtensions.contains(it)
            } ?: mimeType?.let { if (it.endsWith("djvu")) "djvu" else "pdf" } ?: error("Unknown extension $uri")
        }

    }
}

