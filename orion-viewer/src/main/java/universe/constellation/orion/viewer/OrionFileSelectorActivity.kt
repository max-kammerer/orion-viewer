/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import universe.constellation.orion.viewer.Permissions.checkWritePermission
import universe.constellation.orion.viewer.filemanager.FileChooserAdapter
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase
import java.io.File
import java.io.FilenameFilter

class OrionSaveFileActivity : OrionFileManagerActivityBase(
    false,  false,
    FilenameFilter { dir, filename ->
        File(dir, filename).isDirectory
    }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<View>(R.id.saveFileIdView).visibility = View.VISIBLE
        findViewById<Button>(R.id.saveFile).setOnClickListener {
            //should be granted automatically
            if (checkWritePermission(this)) {
                val fileName = findViewById<TextView>(R.id.fileName).text.toString()
                val currentFolder = (findViewById<ListView>(R.id.listView).adapter as FileChooserAdapter).currentFolder
                val targetFile = File(currentFolder, fileName)
                if (targetFile.exists()) {
                    if (targetFile.isDirectory) {
                        AlertDialog.Builder(this).
                        setMessage("Can't save file into '${targetFile.name}' because there is a directory with same name").
                        setPositiveButton("OK") { _, _ -> }.show()
                    }
                    else {
                        AlertDialog.Builder(this).
                        setMessage("File '${targetFile.name}' already exists.\nDo you want to overwrite it?").
                        setPositiveButton("Yes") { _, _ -> saveFile(targetFile) }.
                        setNegativeButton("No") { _, _ -> }.show()
                    }
                } else {
                    saveFile(targetFile)
                }
            }
        }
        //should be granted automatically
        checkWritePermission(this)
    }

    private fun saveFile(targetFile: File) {
        val fileUri = intent.extras.get(SaveNotification.URI) as Uri
        saveFile(targetFile, fileUri)
    }

    private fun saveFile(target: File, fileUri: Uri) {
        SaveNotification.saveFileAndDoAction(this, fileUri, target) {
            openFile(target)
        }
    }
}

class OrionFileSelectorActivity : OrionFileManagerActivityBase(
    false, false,
    FilenameFilter { dir, filename ->
        File(dir, filename).isDirectory || filename.toLowerCase().endsWith(".xml")
    }
) {

    override fun openFile(file: File) {
        val result = Intent()
        result.putExtra(RESULT_FILE_NAME, file.absolutePath)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {

        const val RESULT_FILE_NAME = "RESULT_FILE_NAME"
    }
}
