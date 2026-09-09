package universe.constellation.orion.viewer.djvu

import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.AbstractDocument
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.document.AbstractPage
import universe.constellation.orion.viewer.document.LinkTarget
import universe.constellation.orion.viewer.document.PageLink
import universe.constellation.orion.viewer.document.PageText
import universe.constellation.orion.viewer.document.PageTextBuilder
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.pdf.DocInfo
import universe.constellation.orion.viewer.traceTiming
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DjvuDocument(filePath: String, override val cacheLimit: Long = DEFAULT_CACHE_LIMIT) : AbstractDocument(filePath) {

    inner class DjvuPage(pageNum: Int) : AbstractPage(pageNum) {
        @Volatile
        private var pagePointer: Long = 0

        @Volatile
        private var pageTextBuilder: PageTextBuilder? = null

        override fun readPageSize(): PageSize? {
            if (docPointer == 0L) return errorInDebugOr("Document for $pageNum is null") { null }
            if (destroyed) return errorInDebugOr("Page $pageNum already destroyed") { null }
            return getPageDimension(contextPointer, docPointer, pageNum, PageSize())
        }

        override fun readPageDataForRendering() {
            if (destroyed) return errorInDebug("Page $pageNum already destroyed")
            if (pagePointer == 0L && docPointer != 0L) {
                traceTiming({ "Page extraction: $pageNum" }) {
                    pagePointer = getPageInternal(contextPointer, docPointer, pageNum)
                }
            }
        }

        override fun renderPage(
            bitmap: Bitmap,
            zoom: Double,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            leftOffset: Int,
            topOffset: Int
        ) {
            if (destroyed) return
            readPageDataForRendering()
            if (docPointer == 0L || pagePointer == 0L) return

            traceTiming({
                "Page rendering: $pageNum ($pagePointer) $zoom, ${leftOffset + left}, ${
                    topOffset + top
                }, ${leftOffset + right}, ${topOffset + bottom}"
            }) {
                drawPage(
                    contextPointer,
                    docPointer,
                    pagePointer,
                    bitmap,
                    zoom.toFloat(),
                    bitmap.width,
                    bitmap.height,
                    leftOffset + left,
                    topOffset + top,
                    right - left,
                    bottom - top,
                    left,
                    top
                )
            }
        }

        override fun searchText(text: String): Array<RectF>? {
            return searchPage(this.pageNum, text)
        }

        override fun getPageText(): PageText? {
            if (pageTextBuilder == null) {
                pageTextBuilder = Companion.getText(contextPointer, docPointer, pageNum, PageTextBuilder()) ?: PageTextBuilder.NULL
            }
            val builder = pageTextBuilder
            if (builder == PageTextBuilder.NULL) {
                return null
            }
            return pageTextBuilder
        }

        override fun readLinks(): List<PageLink> {
            if (docPointer == 0L) return emptyList()
            val urls = ArrayList<String>()
            val rects = ArrayList<RectF>()
            getPageLinks(contextPointer, docPointer, pageNum, urls, rects)
            return urls.indices.mapNotNull { resolveLink(urls[it], rects[it], pageNum) }
        }

        override fun destroyInternal() {
            destroyed = true
            releasePage(pagePointer)
            pagePointer = 0
        }

        override fun destroy() {
            destroyPage(this)
        }
    }

    @Volatile
    private var docPointer = 0L
    private var contextPointer = 0L
    private val docInfo = DocInfo()

    override val outline: Array<OutlineItem>?
        get() = getOutline(docPointer)

    init {
        contextPointer = initContext(cacheLimit)
        docInfo.fileName = filePath
        if (contextPointer == 0L) throw RuntimeException("Can't create djvu contextPointer").also { destroy() }
        docPointer = openFile(filePath, contextPointer, docInfo, cacheLimit > 0)
        if (docPointer == 0L) throw RuntimeException("Can't open file $filePath").also { destroy() }
    }

    override val pageCount: Int
        get() = docInfo.pageCount

    override fun createPage(pageNum: Int): AbstractPage {
        return DjvuPage(pageNum)
    }

    /* Guards the native context against a concurrent close: destroy() runs in the background
     * after the controller is gone, trimCache() comes from a memory trim on the main thread. */
    private val lifecycle = ReentrantLock()

    override fun destroy() = lifecycle.withLock {
        destroyPages()
        destroy(contextPointer, docPointer)
        docPointer = 0
        contextPointer = 0
    }

    /* A trim never waits: if the document is being closed, there is nothing left worth trimming. */
    override fun trimCache(keepPercent: Int) {
        if (!lifecycle.tryLock()) return
        try {
            if (contextPointer != 0L) trimCache(contextPointer, keepPercent)
        } finally {
            lifecycle.unlock()
        }
    }

    override val title: String?
        get() = null

    external override fun setContrast(contrast: Int)
    external override fun setThreshold(threshold: Int)

    override fun needPassword() = false

    override fun authenticate(password: String) = true

    private fun searchPage(pageNum: Int, text: String): Array<RectF>? {
        val textToSearch = text.lowercase(Locale.getDefault())
        val strings = ArrayList<String>(500)
        val positions = ArrayList<RectF>(500)

        getPageText(contextPointer, docPointer, pageNum, strings, positions)

        var prevIndex = 0
        val indexes = ArrayList<Int>(500)
        val builder = StringBuilder()
        for (i in positions.indices) {
            val string = strings[i]
            builder.append(string.lowercase(Locale.getDefault()))
            val length = builder.length
            for (j in prevIndex until length) {
                indexes.add(i)
            }
            prevIndex = length
        }

        val searchFrom = 0
        val result = ArrayList<RectF>()
        val textLength = textToSearch.length
        var i = builder.indexOf(textToSearch, searchFrom)
        while (i != -1) {
            val start = indexes[i]
            val end = indexes[i + textLength - 1]

            val rectF = RectF(getSafeRectInPosition(positions, start))
            rectF.union(getSafeRectInPosition(positions, end))
            result.add(rectF)
            i += textLength
            i = builder.indexOf(textToSearch, i)
        }

        return result.toTypedArray()
    }

    /* maparea urls: "#<page ref>" is internal (see [resolvePageRef]), anything else is external. */
    private fun resolveLink(url: String, rect: RectF, currentPage: Int): PageLink? {
        val target = if (url.startsWith("#")) {
            val page = resolvePageRef(url.substring(1), currentPage) ?: return null
            LinkTarget.Internal(page)
        } else {
            LinkTarget.External(url)
        }
        return PageLink(rect.left, rect.top, rect.right, rect.bottom, target)
    }

    /*
     * The same rules and order as djview4's QDjView::pageNumber: "+n"/"-n" are relative to the
     * current page, "n" and the obsolete "$n" are 1-based page numbers, all three clamped to the
     * document instead of dropped; anything else is a component id, name or title looked up by
     * libdjvu, retried without spaces for files written by careless tools.
     */
    private fun resolvePageRef(ref: String, currentPage: Int): Int? {
        if (ref.isEmpty()) return null
        val last = pageCount - 1
        val sign = ref[0]
        if (sign == '+' || sign == '-') {
            val n = ref.substring(1).toIntOrNull() ?: return null
            return (if (sign == '+') currentPage + n else currentPage - n).coerceIn(0, last)
        }
        (if (sign == '$') ref.substring(1) else ref).toIntOrNull()?.let { return (it - 1).coerceIn(0, last) }
        val page = resolvePageByName(docPointer, ref).takeIf { it >= 0 }
            ?: ref.replace(" ", "").takeIf { it != ref }?.let { resolvePageByName(docPointer, it) }
        log("djvu link `#$ref` resolved to page $page")
        return page?.takeIf { it in 0..last }
    }

    private fun getSafeRectInPosition(rects: List<RectF>, position: Int): RectF {
        //TODO
        return rects[position]
    }

    companion object {

        /** For documents opened outside the app (tests); the app sizes it from the device memory. */
        const val DEFAULT_CACHE_LIMIT = 24L shl 20

        init {
            System.loadLibrary("djvu")
            initNative()
        }

        /** Creates a decoding context; a positive [cacheLimit] sizes its cache of decoded files in bytes. */
        @JvmStatic @Synchronized
        external fun initContext(cacheLimit: Long): Long

        /**
         * Evicts cached files down to [keepPercent] of the cache limit, 0 empties the cache.
         * Not under the class lock on purpose: libdjvu guards the cache itself, and the class lock
         * is held for a whole page decode, which the caller (a memory trim on the main thread)
         * must not wait for.
         */
        @JvmStatic
        external fun trimCache(context: Long, keepPercent: Int)

        @JvmStatic @Synchronized
        external fun initNative()

        @JvmStatic @Synchronized
        external fun openFile(filename: String, context: Long, info: DocInfo, useCache: Boolean): Long

        @JvmStatic @Synchronized
        external fun getPageInternal(context: Long, doc: Long, pageNum: Int): Long

        @JvmStatic @Synchronized
        external fun getPageDimension(context: Long, doc: Long, pageNum: Int, info: PageSize): PageSize?

        @JvmStatic
        external fun drawPage(context: Long, doc: Long, page: Long, bitmap: Bitmap, zoom: Float, bitmapWidth: Int, bitmapHeight: Int,
                              patchX: Int, patchY: Int,
                              patchW: Int, patchH: Int,
                              originX: Int, originY: Int): Boolean

        @JvmStatic @Synchronized
        external fun destroy(context: Long, doc: Long)

        @JvmStatic @Synchronized
        external fun getPageText(context: Long, doc: Long, pageNumber: Int, stringBuilder: ArrayList<*>, positions: ArrayList<*>): Boolean

        @JvmStatic @Synchronized
        external fun getOutline(doc: Long): Array<OutlineItem>

        @JvmStatic @Synchronized
        external fun getText(context: Long, doc: Long, pageNumber: Int, pageTextBuilder: PageTextBuilder): PageTextBuilder?

        @JvmStatic @Synchronized
        external fun releasePage(page: Long)

        /** Fills [urls] and [rects] (page coordinates, top-left origin) from the page maparea annotations. */
        @JvmStatic @Synchronized
        external fun getPageLinks(context: Long, doc: Long, pageNumber: Int, urls: ArrayList<*>, rects: ArrayList<*>): Boolean

        /** Zero based page for a page number, component id or title, or -1. */
        @JvmStatic @Synchronized
        external fun resolvePageByName(doc: Long, name: String): Int
    }
}
