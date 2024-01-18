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
import android.graphics.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.DocumentWithCachingImpl
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.layout.CropMargins
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.layout.calcPageLayout
import universe.constellation.orion.viewer.util.ColorUtil
import universe.constellation.orion.viewer.view.PageLayoutManager
import universe.constellation.orion.viewer.view.ViewDimensionAware
import java.util.concurrent.Executors

private const val CACHE_SIZE = 5

class Controller(
    val activity: OrionViewerActivity,
    val document: Document,
    val layoutStrategy: LayoutStrategy,
    val rootJob: Job = Job(),
) : ViewDimensionAware {

    val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    internal var bitmapCache: BitmapCache = BitmapCache()

    private lateinit var layoutInfo: LayoutPosition

    private val listener: DocumentViewAdapter

    private var lastPage = -1

    lateinit var screenOrientation: String

    private var lastScreenSize: Point? = null

    private var contrast: Int = 0
    private var threshold: Int = 0

    private var hasPendingEvents = false

    private val pages = LruCacheWithOnEvict<Int, PageView> (CACHE_SIZE) {
        it.destroy()
    }

    val pageLayoutManager = PageLayoutManager(this, activity.view)

    init {
        log("Creating controller for `$document`")
        listener = object : DocumentViewAdapter() {
            override fun renderingParametersChanged() {
                println("viewParametersChanged")
                if (this@Controller.activity._isResumed) {
                    bitmapCache.invalidateCache()
                    forcePageRecreation()
                    hasPendingEvents = false
                } else {
                    hasPendingEvents = true
                }
            }
        }
        activity.view.pageLayoutManager = pageLayoutManager

        activity.subscriptionManager.addDocListeners(listener)

    }

    fun drawPage(page: Int) {
        layoutStrategy.reset(layoutInfo, page)
        drawPage(layoutInfo)
    }

    fun forcePageRecreation() {
        pages.snapshot().values.forEach {
            it.invalidateAndUpdate()
        }
    }

    @JvmOverloads
    fun drawPage(info: LayoutPosition = layoutInfo) {
        log("Draw page " + document.title + " "+ info.pageNumber)
        layoutInfo = info
        pageLayoutManager.renderPageWithOffset(info.pageNumber, info.x.offset, info.y.offset)
    }

    fun processPendingEvents() {
        if (hasPendingEvents) {
            log("Processing pending updates...")
            sendViewChangeNotification()
        }
    }

    override fun onDimensionChanged(newWidth: Int, newHeight: Int) {
        if (newWidth > 0 && newHeight > 0) {
            log("New screen size ${newWidth}x$newHeight")
            pageLayoutManager.onDimensionChanged(newWidth, newHeight)
            layoutStrategy.setViewSceneDimension(newWidth, newHeight)
            val options = activity.globalOptions
            layoutStrategy.changeOverlapping(options.horizontalOverlapping, options.verticalOverlapping)
            val offsetX = layoutInfo.x.offset
            val offsetY = layoutInfo.y.offset
            layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
            lastScreenSize = lastScreenSize?.let {  screenSize ->
                if (newWidth == screenSize.x && newHeight == screenSize.y) {
                    layoutInfo.x.offset = offsetX
                    layoutInfo.y.offset = offsetY
                }
                null
            }
            sendViewChangeNotification()
        }
    }

    fun drawNext() {
        layoutStrategy.calcPageLayout(layoutInfo, true, pageCount)
        drawPage(layoutInfo)
    }

    fun drawPrev() {
        layoutStrategy.calcPageLayout(layoutInfo, false, pageCount)
        drawPage(layoutInfo)
    }

    fun translateAndZoom(changeZoom: Boolean, zoomScaling: Float, deltaX: Float, deltaY: Float) {
        log("zoomscaling  $changeZoom $zoomScaling  $deltaX  $deltaY")
        val oldOffsetX = layoutInfo.x.offset
        val oldOffsetY = layoutInfo.y.offset
        log("oldZoom  " + layoutInfo.docZoom + "  " + layoutInfo.x.offset + " x " + layoutInfo.y.offset)

        if (changeZoom) {
            layoutStrategy.changeZoom((10000.0f * zoomScaling * layoutInfo.docZoom).toInt())
            layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
        }

        layoutInfo.x.offset = (zoomScaling * oldOffsetX + deltaX).toInt()
        layoutInfo.y.offset = (zoomScaling * oldOffsetY + deltaY).toInt()
        log("newZoom  " + layoutInfo.docZoom + "  " + layoutInfo.x.offset + " x " + layoutInfo.y.offset)

        sendViewChangeNotification()
    }

    fun changeZoom(zoom: Int) {
        if (layoutStrategy.changeZoom(zoom)) {
            layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
            sendViewChangeNotification()
        }
    }

    val zoom10000Factor: Int
        get() = layoutStrategy.zoom

    val currentPageZoom: Double
        get() = layoutInfo.docZoom

    fun changeCropMargins(cropMargins: CropMargins) {
        if (layoutStrategy.changeCropMargins(cropMargins)) {
            layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
            if (document is DocumentWithCachingImpl) {
                document.resetCache()
            }
            sendViewChangeNotification()
        }
    }

    val margins: CropMargins
        get() = layoutStrategy.margins

    fun destroy() {
        activity.subscriptionManager.unSubscribe(listener)
        pageLayoutManager.destroy()
        pages.evictAll()
        GlobalScope.launch(Dispatchers.Default) {
            log("Destroying controller for ${document}...")
            rootJob.cancelAndJoin()
            document.destroy()
        }
    }

    fun onPause() {
    }

    fun changeOverlap(horizontal: Int, vertical: Int) {
        if (layoutStrategy.changeOverlapping(horizontal, vertical)) {
            layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
            sendViewChangeNotification()
        }
    }

    var rotation: Int
        get() = layoutStrategy.rotation
        set(rotation) {
            if (layoutStrategy.changeRotation(rotation)) {
                layoutStrategy.reset(layoutInfo, layoutInfo.pageNumber)
                sendViewChangeNotification()
            }
        }

    val currentPage: Int
        get() = layoutInfo.pageNumber

    val pageCount: Int
        get() = document.pageCount


    fun init(info: LastPageInfo, dimension: Point) {
        task("init controller") {
            document.setContrast(info.contrast)
            document.setThreshold(info.threshold)

            layoutStrategy.init(info, activity.globalOptions)
            layoutInfo = LayoutPosition()
            layoutStrategy.reset(layoutInfo, info.pageNumber)
            layoutInfo.x.offset = info.newOffsetX
            layoutInfo.y.offset = info.newOffsetY

            lastScreenSize = Point(info.screenWidth, info.screenHeight)
            changeOrinatation(screenOrientation)
            changeColorMode(info.colorMode, false)

            onDimensionChanged(dimension.x, dimension.y)
        }
    }

    fun serializeAndSave(info: LastPageInfo, activity: Activity) {
        layoutStrategy.serialize(info)
        info.newOffsetX = layoutInfo.x.offset
        info.newOffsetY = layoutInfo.y.offset
        info.pageNumber = layoutInfo.pageNumber
        info.screenOrientation = screenOrientation
        info.save(activity)
    }

    private fun sendViewChangeNotification() {
        activity.subscriptionManager.sendViewChangeNotification()
    }

    private fun sendPageChangedNotification() {
        if (lastPage != layoutInfo.pageNumber) {
            lastPage = layoutInfo.pageNumber
            activity.subscriptionManager.sendPageChangedNotification(lastPage, document.pageCount)
        }
    }

    val direction: String
        get() = layoutStrategy.walker.order.name

    val layout: Int
        get() = layoutStrategy.layout

    fun setDirectionAndLayout(walkOrder: String, pageLayout: Int) {
        if (layoutStrategy.changeWalkOrder(walkOrder) or layoutStrategy.changePageLayout(pageLayout)) {
            sendViewChangeNotification()
        }
    }

    fun changetWalkOrder(walkOrder: String) {
        if (layoutStrategy.changeWalkOrder(walkOrder)) {
            sendViewChangeNotification()
        }
    }

    fun changetPageLayout(pageLayout: Int) {
        if (layoutStrategy.changePageLayout(pageLayout)) {
            sendViewChangeNotification()
        }
    }

    fun changeContrast(contrast: Int) {
        if (this.contrast != contrast) {
            this.contrast = contrast
            document.setContrast(contrast)
            sendViewChangeNotification()
        }
    }

    fun changeThreshhold(threshold: Int) {
        if (this.threshold != threshold) {
            this.threshold = threshold
            document.setThreshold(threshold)
            sendViewChangeNotification()
        }
    }

    //zero based
    val isEvenPage: Boolean
        get() = currentPage.isZeroBasedEvenPage

    fun changeOrinatation(orientationId: String) {
        screenOrientation = orientationId
        log("New orientation $screenOrientation")
        var realOrintationId = orientationId
        if ("DEFAULT" == orientationId) {
            realOrintationId = activity.applicationDefaultOrientation
        }
        activity.changeOrientation(activity.getScreenOrientation(realOrintationId))
    }

    fun changeColorMode(colorMode: String, invalidate: Boolean) {
        activity.fullScene.setColorMatrix(ColorUtil.getColorMode(colorMode))
        if (invalidate) {
            activity.view.invalidate()
        }
    }

    val outline: Array<OutlineItem>?
        get() = document.outline

    fun selectRawText(pageNum: Int, startX: Int, startY: Int, widht: Int, height: Int, isSingleWord: Boolean): String? {
        return document.getText(
            pageNum, startX, startY, widht, height, isSingleWord
        )?.trimText(isSingleWord)
    }

    private fun String.trimText(isSingleWord: Boolean): String {
        return if (!isSingleWord) trim()
        else this.dropWhile { it.isWhitespace() || it in PUNCTUATION_CHARS }.dropLastWhile { it.isWhitespace() || it in PUNCTUATION_CHARS }
    }

    fun needPassword(): Boolean {
        return document.needPassword()
    }

    fun authenticate(password: String): Boolean {
        return document.authenticate(password).also {
            if (it) {
                sendViewChangeNotification()
            }
        }
    }

    fun createCachePageView(pageNum: Int): PageView {
        val pageView = pages.get(pageNum)
        if (pageView != null) {
            if (pageView.state == State.CAN_BE_DELETED) {
                pageView.reinit() //TODO: split bitmap invalidation and page unload
            }
            return pageView
        } else {
            println("create page $pageNum")
            val pageView = PageView(
                pageNum,
                document,
                controller = this,
                rootJob = rootJob,
                pageLayoutManager = pageLayoutManager
            ).apply { init() }
            pages.put(pageNum, pageView)
            return pageView
        }
    }

    companion object {
        //https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
        val PUNCTUATION_CHARS = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet()
    }
}
