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

@file:Suppress("NOTHING_TO_INLINE")
package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.timing
import java.util.concurrent.atomic.AtomicInteger

abstract class PageWithAutoCrop(override val pageNum: Int) : Page {

    private val counter = AtomicInteger(0)

    @Volatile
    protected var destroyed = false

    @Volatile
    private lateinit var pageSize: PageSize

    fun increaseUsages() {
        counter.incrementAndGet()
    }

    fun decreaseUsages(): Int {
        return counter.decrementAndGet()
    }

    protected abstract fun readPageSize(): PageSize?

    override fun getPageSize(): PageSize {
        if (!::pageSize.isInitialized) {
            timing("Page $pageNum size extraction") {
                pageSize = readPageSize() ?: dimensionForCorruptedPage().also {
                    logError("Page $pageNum is corrupted")
                }
            }
            log("Page $pageNum size: $pageSize")
        }
        return pageSize
    }

    private fun dimensionForCorruptedPage() = PageSize(300, 400)

    override fun toString(): String {
        return "Page $pageNum"
    }

    abstract fun destroyInternal()
}