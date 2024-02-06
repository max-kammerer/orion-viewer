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
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.DisplayMetrics
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import universe.constellation.orion.viewer.bitmap.DeviceInfo
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

private const val CACHE_SIZE = 10

class Controller(
    val activity: OrionViewerActivity,
    val document: Document,
    val layoutStrategy: LayoutStrategy,
    val rootJob: Job = Job(),
) : ViewDimensionAware {

    val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    internal var bitmapCache: BitmapCache = BitmapCache()

    private val layoutInfo: LayoutPosition?
        get() = pageLayoutManager.currentPageLayout()

    private val listener: DocumentViewAdapter

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
                hasPendingEvents = if (this@Controller.activity._isResumed) {
                    bitmapCache.invalidateCache()
                    pageLayoutManager.forcePageUpdate()
                    false
                } else {
                    true
                }
            }
        }
        activity.view.pageLayoutManager = pageLayoutManager

        activity.subscriptionManager.addDocListeners(listener)

    }

    @JvmOverloads
    fun drawPage(pageNum: Int, pageXOffset: Int = 0, pageYOffset: Int = 0): Deferred<PageView?> {
        log("Controller drawPage $document $pageNum: $pageXOffset $pageYOffset")
        return pageLayoutManager.renderPageAt(pageNum, -pageXOffset, -pageYOffset)
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
            layoutStrategy.setViewSceneDimension(newWidth, newHeight)
            val options = activity.globalOptions
            layoutStrategy.changeOverlapping(options.horizontalOverlapping, options.verticalOverlapping)
            pageLayoutManager.onDimensionChanged(newWidth, newHeight)
            sendViewChangeNotification()
        }
    }

    fun drawNext(): Deferred<PageView?>? {
        layoutInfo?.let {
            val copy = it.copy()
            layoutStrategy.calcPageLayout(copy, true, pageCount)
            return drawPage(copy.pageNumber, copy.x.offset, copy.y.offset)
        } ?: run {
            log("Problem: no visible page invoking drawNext")
            return null
        }
    }

    fun drawPrev(): Deferred<PageView?>? {
        layoutInfo?.let {
            val copy = it.copy()
            layoutStrategy.calcPageLayout(copy, false, pageCount)
            log("Controller drawPrev ${copy.pageNumber} $document: ${copy.x.offset} ${copy.y.offset}")
            return drawPage(copy.pageNumber, copy.x.offset, copy.y.offset)
        } ?: run {
            log("Problem: no visible page invoking drawPrev")
            return null
        }
    }

    fun translateAndZoom(zoomScaling: Float, startFocus: PointF, endFocus: PointF, deltaX: Float, deltaY: Float) {
        layoutInfo?.let {
            layoutStrategy.changeZoom((10000.0f * zoomScaling * it.docZoom).toInt())
            pageLayoutManager.performTouchZoom(zoomScaling, startFocus, endFocus)
            //TODO split notification into page geometry and book info
            //sendViewChangeNotification()
        }
    }

    fun changeZoom(zoom: Int) {
        if (layoutStrategy.changeZoom(zoom)) {
            sendViewChangeNotification()
        }
    }

    val zoom10000Factor: Int
        get() = layoutStrategy.zoom

    val currentPageZoom: Double
        get() = layoutInfo?.docZoom ?: 1.0

    fun changeCropMargins(cropMargins: CropMargins) {
        if (layoutStrategy.changeCropMargins(cropMargins)) {
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
            log("Destroying controller for $document...")
            rootJob.cancelAndJoin()
            document.destroy()
        }
    }

    fun onPause() {
    }

    fun changeOverlap(horizontal: Int, vertical: Int) {
        if (layoutStrategy.changeOverlapping(horizontal, vertical)) {
            sendViewChangeNotification()
        }
    }

    var rotation: Int
        get() = layoutStrategy.rotation
        set(rotation) {
            if (layoutStrategy.changeRotation(rotation)) {
                sendViewChangeNotification()
            }
        }

    val currentPage: Int
        get() = pageLayoutManager.currentPageLayout()?.pageNumber ?: -1

    val pageCount: Int
        get() = document.pageCount


    fun init(info: LastPageInfo, dimension: Point) {
        task("init controller") {
            document.setContrast(info.contrast)
            document.setThreshold(info.threshold)

            layoutStrategy.init(info, activity.globalOptions)

            lastScreenSize = Point(info.screenWidth, info.screenHeight)
            changeOrinatation(screenOrientation)
            changeColorMode(info.colorMode, false)

            onDimensionChanged(dimension.x, dimension.y)
        }
    }

    fun serializeAndSave(info: LastPageInfo, activity: Activity) {
        layoutStrategy.serialize(info)
        info.newOffsetX = layoutInfo?.x?.offset ?: 0
        info.newOffsetY = layoutInfo?.y?.offset ?: 0
        info.pageNumber = layoutInfo?.pageNumber ?: 0
        info.screenOrientation = screenOrientation
        info.save(activity)
    }

    private fun sendViewChangeNotification() {
        activity.subscriptionManager.sendViewChangeNotification()
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
            if (pageView.state == PageState.CAN_BE_DELETED) {
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

    fun drawPage(lp: LayoutPosition) {
        drawPage(lp.pageNumber, lp.x.offset, lp.y.offset)
    }

    companion object {
        //https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
        val PUNCTUATION_CHARS = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet()
    }

    fun getDeviceInfo(): DeviceInfo {
        val am = activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        Runtime.getRuntime().maxMemory() / 1024
        Runtime.getRuntime().totalMemory()
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(dm);

        val width = dm.widthPixels
        val height = dm.heightPixels
        return DeviceInfo(am.memoryClass, width, height, Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
    }

    override fun toString(): String {
        return "Controller for $document (controller identity hashCode=${System.identityHashCode(this)}})"
    }
}
