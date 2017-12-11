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

import android.graphics.Bitmap
import android.os.Debug
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.view.Renderer
import universe.constellation.orion.viewer.view.Scene

/**
 * User: mike
 * Date: 19.10.11
 * Time: 9:52
 */
open class RenderThread(private val activity: OrionViewerActivity, protected var layout: LayoutStrategy, protected var doc: Document, private val executeInSeparateThread: Boolean, private val fullScene: Scene) : Thread(), Renderer {

    private val cachedBitmaps = LinkedList<CacheInfo>()

    private var currentPosition: LayoutPosition? = null

    private var lastEvent: LayoutPosition? = null

    private var stopped: Boolean = false

    internal constructor(activity: OrionViewerActivity, layout: LayoutStrategy, doc: Document, fullScene: Scene) : this(activity, layout, doc, true, fullScene) {}

    init {
        log("RenderThread was created successfully")
    }

    override fun invalidateCache() {
        synchronized(this) {
            for (next in cachedBitmaps) {
                next.isValid = false
            }
            log("Cache invalidated")
        }
    }

    override fun startRenderer() {
        log("Starting renderer")
        start()
    }

    fun cleanCache() {
        synchronized(this) {
            log("Allocated heap size: " + memoryInMB(Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()))
            cachedBitmaps.clear()
            log("Cache is cleared!")
            currentPosition = null
        }
    }

    override fun stopRenderer() {
        synchronized(this) {
            stopped = true
            cleanCache()
            (this as java.lang.Object).notify()
        }
    }

    override fun onPause() {
        //        synchronized (this) {
        //            paused = true;
        //        }
    }

    override fun onResume() {
        synchronized(this) {
            (this as java.lang.Object).notify()
        }
    }

    override fun run() {
        var futureIndex = 0
        var curPos: LayoutPosition? = null

         while (!stopped) {

            log("Allocated heap size " + memoryInMB(Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()))

            var rotation = 0
            val doContinue = synchronized(this) {
                if (lastEvent != null) {
                    currentPosition = lastEvent
                    lastEvent = null
                    futureIndex = 0
                    curPos = currentPosition
                }

                //keep it here
                rotation = layout.rotation

                if (currentPosition == null || futureIndex > FUTURE_COUNT || currentPosition!!.screenWidth == 0 || currentPosition!!.screenHeight == 0) {
                    try {
                        log("WAITING...")
                        (this as java.lang.Object).wait()
                    } catch (e: InterruptedException) {
                        log(e)
                    }

                    log("AWAKENING!!!")
                    true
                } else {
                    //will cache next page
                    log("Future index is " + futureIndex)
                    if (futureIndex != 0) {
                        curPos = curPos!!.clone()
                        layout.calcPageLayout( curPos!!, true, doc.pageCount)
                    }
                    false
                }
            }
             if (doContinue) continue

            log("rotation = $rotation")
            renderInCurrentThread(futureIndex == 0, curPos, rotation)
            futureIndex++
        }
    }

    protected fun renderInCurrentThread(flushBitmap: Boolean, curPos: LayoutPosition?, rotation: Int): Bitmap {
        var resultEntry: CacheInfo? = null
        log("Orion: rendering position: $curPos")
        if (curPos != null) {
            //try to find result in cache
            val iterator = cachedBitmaps.iterator()
            while (iterator.hasNext()) {
                val cacheInfo = iterator.next()
                if (cacheInfo.isValid && cacheInfo.info == curPos) {
                    resultEntry = cacheInfo
                    //relocate info to end of cache
                    iterator.remove()
                    cachedBitmaps.add(cacheInfo)
                    break
                }
            }


            if (resultEntry == null) {
                //render page
                resultEntry = render(curPos, rotation)

                synchronized(this) {
                    cachedBitmaps.add(resultEntry!!)
                }
            }


            if (flushBitmap) {
                val bitmap = resultEntry.bitmap
                log("Sending Bitmap")
                val mutex = CountDownLatch(1)

                if (!executeInSeparateThread) {
                    fullScene.onNewImage(bitmap, curPos, mutex)
                    activity.getDevice().flushBitmap()
                    mutex.countDown()
                } else {
                    activity.runOnUiThread {
                        fullScene.onNewImage(bitmap, curPos, mutex)
                        activity.getDevice().flushBitmap()
                    }
                }

                try {
                    mutex.await(1, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    log(e)
                }

            }
        }

        return resultEntry!!.bitmap
    }

    private fun render(curPos: LayoutPosition, rotation: Int): CacheInfo {
        val resultEntry: CacheInfo
        val width = curPos.x.screenDimension
        val height = curPos.y.screenDimension

        val screenWidth = curPos.screenWidth
        val screenHeight = curPos.screenHeight

        var bitmap: Bitmap? = null
        if (cachedBitmaps.size >= CACHE_SIZE) {
            val info = cachedBitmaps.removeFirst()
            info.isValid = false

            if (screenWidth == info.bitmap.width && screenHeight == info.bitmap.height /*|| rotation != 0 && width == info.bitmap.getHeight() && height == info.bitmap.getWidth()*/) {
                bitmap = info.bitmap
            } else {
                info.bitmap.recycle() //todo recycle from ui
            }
        }
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        } else {
            log("Using cached bitmap " + bitmap)
        }

        val leftTopCorner = layout.convertToPoint(curPos)


        doc.renderPage(curPos.pageNumber, bitmap!!, curPos.docZoom, leftTopCorner.x, leftTopCorner.y, leftTopCorner.x + width, leftTopCorner.y + height)

        resultEntry = CacheInfo(curPos, bitmap)
        return resultEntry
    }

    override fun render(lastInfo: LayoutPosition) {
        val lastInfo = lastInfo.clone()
        synchronized(this) {
            lastEvent = lastInfo
            (this as java.lang.Object).notify()
        }
    }

    private class CacheInfo(val info: LayoutPosition, val bitmap: Bitmap) {
        var isValid = true
    }

    companion object {
        private const val CACHE_SIZE = 4

        private const val FUTURE_COUNT = 1
    }
}
