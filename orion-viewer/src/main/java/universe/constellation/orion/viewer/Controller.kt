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

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.DisplayMetrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.bitmap.DeviceInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.document.lastPageNum0
import universe.constellation.orion.viewer.layout.CropMargins
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
import universe.constellation.orion.viewer.util.ColorUtil
import universe.constellation.orion.viewer.view.PageLayoutManager
import universe.constellation.orion.viewer.view.PageView
import universe.constellation.orion.viewer.view.ViewDimensionAware

class Controller(
    val activity: OrionViewerActivity,
    val document: Document,
    val layoutStrategy: LayoutStrategy,
    private val rootJob: Job = Job(),
    val context: CoroutineDispatcher = Dispatchers.Default
) : ViewDimensionAware {

    val scope = CoroutineScope(context + rootJob)

    internal var bitmapCache: BitmapCache = BitmapCache()

    private val layoutInfo: LayoutPosition?
        get() = pageLayoutManager.currentPageLayout()

    private val listener: DocumentViewAdapter

    lateinit var screenOrientation: String

    private var lastScreenSize: Point? = null

    private var contrast: Int = 0
    private var threshold: Int = 0

    private var hasPendingEvents = false

    lateinit var pageLayoutManager: PageLayoutManager

    init {
        log("Creating controller for `$document`")
        listener = object : DocumentViewAdapter() {
            override fun renderingParametersChanged() {
                println("viewParametersChanged")
                hasPendingEvents = if (this@Controller.activity._isResumed) {
                    pageLayoutManager.forcePageUpdate()
                    false
                } else {
                    true
                }
            }
        }
    }

    @JvmOverloads
    fun drawPage(pageNum: Int, pageXOffset: Int = 0, pageYOffset: Int = 0, isTapNavigation: Boolean = false): Pair<PageView, Job> {
        log("Controller drawPage $document $pageNum: $pageXOffset $pageYOffset")
        return pageLayoutManager.renderPageAt(pageNum, -pageXOffset, -pageYOffset, isTapNavigation)
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

    fun drawNext(): Pair<PageView, Job>? {
        return pageLayoutManager.renderNextOrPrev(true, isTapNavigation = true)
    }

    fun drawPrev(): Pair<PageView, Job>? {
        return pageLayoutManager.renderNextOrPrev(false, isTapNavigation = true)
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
            //TODO: cache auto crop and reset it here
            sendViewChangeNotification()
        }
    }

    val margins: CropMargins
        get() = layoutStrategy.margins

    fun destroy() {
        activity.subscriptionManager.unSubscribe(listener)
        activity.view.pageLayoutManager = null

        if (::pageLayoutManager.isInitialized)
            pageLayoutManager.destroy()

        GlobalScope.launch(Dispatchers.Default) {
            log("Destroying controller for $document...")
            rootJob.cancelAndJoin()
            if (context != Dispatchers.Default && context is ExecutorCoroutineDispatcher) {
                context.close()
            }
            document.destroy()
            bitmapCache.free()
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
        get() = pageLayoutManager.currentPageLayout()?.pageNumber
            ?: errorInDebugOr("No active page $document") { 0 }

    val pageCount: Int
        get() = document.pageCount


    fun init(info: LastPageInfo, viewWidth: Int, viewHeight: Int) {
        task("init controller: $viewWidth $viewHeight") {
            document.setContrast(info.contrast)
            document.setThreshold(info.threshold)

            layoutStrategy.init(info, activity.globalOptions)

            lastScreenSize = Point(info.screenWidth, info.screenHeight)
            changeOrinatation(screenOrientation)
            changeColorMode(info.colorMode, false)

            pageLayoutManager = PageLayoutManager(this, activity.view)
            activity.subscriptionManager.addDocListeners(listener)
            activity.view.pageLayoutManager = pageLayoutManager

            onDimensionChanged(viewWidth, viewHeight)
        }
    }

    fun serializeAndSave(info: LastPageInfo, activity: OrionBaseActivity) {
        layoutStrategy.serialize(info)
        pageLayoutManager.serialize(info)
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

    fun getOutline(): Array<OutlineItem>? = document.outline

    fun selectRawText(page: Page, startX: Int, startY: Int, widht: Int, height: Int, isSingleWord: Boolean): String? {
        return page.getText(startX, startY, widht, height, isSingleWord)?.trimText(isSingleWord)
            ?.apply { log("Text selection in ${page.pageNum}: $this") }
    }

    private fun String.trimText(isSingleWord: Boolean): String {
        return if (!isSingleWord) trim()
        else this.dropWhile { it.isWhitespace() || it in PUNCTUATION_CHARS }.dropLastWhile { it.isWhitespace() || it in PUNCTUATION_CHARS }
    }

    fun needPassword(): Boolean {
        return document.needPassword()
    }

    suspend fun authenticate(password: String): Boolean {
        val result = withContext(context) { document.authenticate(password) }
        if (result) {
            sendViewChangeNotification()
        }
        return result
    }

    fun createPageView(pageNum: Int): PageView {
        return PageView(
            pageNum,
            document,
            controller = this,
            rootJob = rootJob,
            pageLayoutManager = pageLayoutManager
        ).apply { init() }
    }

    @JvmOverloads
    fun drawPage(lp: LayoutPosition, isTapNavigation: Boolean = false): Pair<PageView, Job> {
        return drawPage(lp.pageNumber, lp.x.offset, lp.y.offset, isTapNavigation)
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

    suspend fun <T> runInBackground(body: Controller.() -> T): T {
        return withContext(context + rootJob) {
            this@Controller.body()
        }
    }

    fun runInScope(body: suspend Controller.() -> Unit) {
        scope.launch {
            this@Controller.body()
        }
    }
}

val Controller.lastPageNum0
    get() = document.lastPageNum0
