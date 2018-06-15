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
import android.os.Bundle
import android.view.View
import android.widget.TextView
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import java.io.File
import java.io.FilenameFilter

class OrionSaveFileActivity : OrionFileManagerActivity(
    false,  false,
    FilenameFilter { dir, filename ->
        File(dir, filename).isDirectory
    }) {

    override fun onNewIntent(intent: Intent) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<TextView>(R.id.fileName).visibility = View.VISIBLE
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
