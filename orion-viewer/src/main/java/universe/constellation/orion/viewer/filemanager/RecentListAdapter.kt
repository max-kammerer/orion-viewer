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
import universe.constellation.orion.viewer.prefs.GlobalOptions
import java.util.Locale

class RecentListAdapter(context: Context, val globalOptions: GlobalOptions) :
    ArrayAdapter<GlobalOptions.RecentEntry>(context, R.layout.recent_entry, R.id.fileName, globalOptions.recentFiles) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val newConvertView = super.getView(position, convertView, parent)
        getItem(position)?.let { entry ->
            val name = entry.lastPathElement

            val icon = getIconByNameExtension(name)
            val iconView = newConvertView.findViewById<ImageView>(R.id.fileImage)!!
            iconView.setImageResource(icon)

            val fileName = newConvertView.findViewById<TextView>(R.id.fileName)!!
            fileName.text = name

            val delete = newConvertView.findViewById<ImageView>(R.id.trash)
            delete.setOnClickListener {it
                this.remove(entry)
                globalOptions.removeRecentEntry(entry)
            }
        }

        return newConvertView
    }
}

fun getIconByNameExtension(name: String): Int {
    return when (name.fileExtensionLC) {
        "pdf" -> R.drawable.pdf
        "djvu", "djv" -> R.drawable.djvu
        "xps", "oxps" -> R.drawable.xps
        "xml" -> R.drawable.xml
        "cbz", "tif", "tiff" -> R.drawable.cbz
        else -> R.drawable.cbz
    }
}

val String.fileExtension
    get() = substringAfterLast('.', "")

val String.fileExtensionLC
    get() = fileExtension.lowercase(Locale.getDefault())