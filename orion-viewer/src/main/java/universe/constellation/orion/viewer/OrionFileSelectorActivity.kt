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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.widget.toast

import java.io.File
import java.io.FilenameFilter

import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity

class OrionSaveFileActivity : OrionFileManagerActivity(
    false,  false,
    FilenameFilter { dir, filename ->
        File(dir, filename).isDirectory
    }) {

    private lateinit var uri: String

    override fun onNewIntent(intent: Intent) {
        uri = intent.getStringExtra(SaveNotification.URI)!!
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        uri = intent.getStringExtra(SaveNotification.URI)!!
        onOrionCreate(savedInstanceState, R.layout.file_selector, true)
        val fileNameView = findViewById<TextView>(R.id.fileName)
        fileNameView.visibility = View.VISIBLE
        val button = findViewById<TextView>(R.id.saveFile)
        button.visibility = View.VISIBLE
        button.setOnClickListener { view ->
            val fileName = fileNameView.text
            if (fileName.isBlank()) {
                toast("File name is blank").show()
            }
            else {
                val folders = findMyViewById(R.id.folders)
                val folder = folders.findViewById<TextView>(R.id.path).text

                SaveNotification.saveFileAndInvoke(uri.toUri(), File(folder.toString(), fileName.toString()), this) {
                    openFile(File(it))
                }
            }
        }
    }
}

class OrionFileSelectorActivity : OrionFileManagerActivity(
    false, false,
    FilenameFilter { dir, filename ->
        File(dir, filename).isDirectory || filename.toLowerCase().endsWith(".xml")
    }
) {

    override fun onNewIntent(intent: Intent) {

    }

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
