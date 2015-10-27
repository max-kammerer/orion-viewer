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

package universe.constellation.orion.viewer.document

import android.support.v4.util.LruCache
import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.PageInfo

/**
 * User: mike
 * Date: 15.10.11
 * Time: 9:53
 */
class DocumentWithCaching(val doc: DocumentWrapper) : DocumentWrapper by doc {

    private val cache = LruCache<Int, PageInfo?>(100)

    override fun getPageInfo(pageNum: Int): PageInfo? {
        var pageInfo = cache.get(pageNum)
        if (pageInfo == null) {
            pageInfo = doc.getPageInfo(pageNum)
            cache.put(pageNum, pageInfo)
        }
        return pageInfo
    }
}