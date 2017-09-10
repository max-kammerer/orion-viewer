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

package universe.constellation.orion.viewer.view

import android.graphics.Canvas
import android.graphics.Rect
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

interface ScreenInfo {
    val sceneWidth: Int
    val sceneHeight: Int
}

interface Context {
    val continuation: CoroutineContext
    val rootJob: Job
}


class Page(
        val number: Int,
        val position: Rect,
        val info: ScreenInfo,
        val doc: Document,
        val context: Context
) : DrawTask  {

    val pageJob = Job()

    val pageInfo = PageInfo(0, info.sceneWidth, info.sceneHeight)



    val pageInfoRaw: Deferred<PageInfo> = async(context.continuation) {
        if (isActive) {
            doc.getPageInfo(number, 0).also {
                launch(UI) {
                    pageInfo.width = it.width
                    pageInfo.height = it.height
                }
            }
        } else
            throw CancellationException()
    }

    val page: Deferred<Long> = async(context.continuation) {
        if (isActive) {
            doc.loadPage(number)
        } else
            throw CancellationException()
    }

    val visibleRect = Rect()
        get() = field.apply {
            set(0, 0, info.sceneWidth, info.sceneHeight)
            intersect(position)
        }

    fun invalidate() {

    }

    val isVisible = visibleRect.intersect(position)

    override fun drawOnCanvas(canvas: Canvas, stuff: ColorStuff, drawContext: DrawContext) {
        canvas.ren
    }

    fun destroy() {
        //TODO mutext
        async(continuation) {
            if (page.isActive || page.isCompleted) {
                doc.releasePage(page.await())
            } else {
                page.cancel()
            }
        }
    }
}