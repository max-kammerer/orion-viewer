/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.filemanager

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import universe.constellation.orion.viewer.R
import java.io.File
import java.io.FilenameFilter
import java.util.*

class FileChooserAdapter(
    context: Context,
    startFolder: File,
    private val filter: FilenameFilter
) : ArrayAdapter<File>(context, R.layout.file_entry, R.id.fileName) {

    private var currentList = arrayListOf<File>()

    private val parentFile = File("..")

    var currentFolder: File = startFolder
        private set

    init {
        changeFolder(currentFolder)
    }

    fun changeFolder(file: File): File {
        val newFolder = changeFolderInner(file)
        this.notifyDataSetChanged()
        return newFolder
    }


    private fun changeFolderInner(file: File): File {
        currentList.clear()
        val newFile = if (file === parentFile) currentFolder.parentFile else file
        currentFolder = newFile

        if (newFile.parent != null) currentList.add(parentFile)

        newFile.listFiles(filter)?.let {
            currentList.addAll(it)
        }

        currentList.sortWith (
            Comparator { f1, f2 ->
                if (f1.isDirectory && !f2.isDirectory || !f1.isDirectory && f2.isDirectory) {
                    return@Comparator if (f1.isDirectory) -1 else 1
                }
                f1.name.compareTo(f2.name)
            }
        )

        return currentFolder
    }

    override fun getCount(): Int {
        return currentList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val newConvertView = super.getView(position, convertView, parent)
        getItem(position)?.let { data ->
            val isDirectory = data.isDirectory
            val name = data.name

            val icon = if(isDirectory) R.drawable.folder else getIconByNameExtension(name)
            val fileIcon = newConvertView.findViewById<ImageView>(R.id.fileImage)
            fileIcon.setImageResource(icon)

            val fileName = newConvertView.findViewById<TextView>(R.id.fileName)
            fileName.text = name
        }

        return newConvertView
    }

    override fun getItem(position: Int): File? {
        return currentList[position]
    }

    companion object {

        val supportedExtensions = setOf("cbz", "djvu", "djv", "pdf", "oxps", "tiff", "tif", "xps")

        @JvmField
        var DEFAULT_FILTER: FilenameFilter = FilenameFilter { dir, filename ->
            if (File(dir, filename).isDirectory) {
                return@FilenameFilter true
            }
            if (filename.startsWith("._") || filename == ".DS_Store") {
                return@FilenameFilter false
            }

            supportedExtensions.contains(filename.fileExtension.lowercase(Locale.getDefault()))
        }
    }
}
