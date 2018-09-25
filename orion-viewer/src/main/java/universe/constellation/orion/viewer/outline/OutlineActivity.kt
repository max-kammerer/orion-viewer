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

package universe.constellation.orion.viewer.outline

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import universe.constellation.orion.viewer.OrionBookmarkActivity
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.prefs.OrionApplication

class OutlineActivity : ListActivity() {
    private lateinit var items: Array<OutlineItem>

    private val orionContext: OrionApplication
        get() = applicationContext as OrionApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        orionContext.applyTheme(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        items = (applicationContext as OrionApplication).tempOptions!!.outline
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        val result = Intent()
        result.putExtra(OrionBookmarkActivity.OPEN_PAGE, items[position].page)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
