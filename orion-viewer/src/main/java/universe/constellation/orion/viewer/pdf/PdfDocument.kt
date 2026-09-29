/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017  Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.pdf

import android.graphics.RectF
import androidx.core.graphics.toRect
import com.artifex.mupdf.fitz.Device
import com.artifex.mupdf.fitz.Document as FitzDocument
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Link
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.PDFObject
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.artifex.mupdf.viewer.MuPDFCore
import com.artifex.mupdfdemo.TextWord
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.device.calcFZCacheSize
import universe.constellation.orion.viewer.document.AbstractDocument
import universe.constellation.orion.viewer.document.AbstractPage
import universe.constellation.orion.viewer.document.LinkTarget
import universe.constellation.orion.viewer.document.NonFatalErrorReporter
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.document.PageLink
import universe.constellation.orion.viewer.document.PageText
import universe.constellation.orion.viewer.document.PageTextBuilder
import universe.constellation.orion.viewer.errorInDebug
import universe.constellation.orion.viewer.errorInDebugOr
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.describeFileHeader
import universe.constellation.orion.viewer.mupdfLoaded
import universe.constellation.orion.viewer.shrinkMupdfStore
import universe.constellation.orion.viewer.traceTiming

class PdfDocument @Throws(Exception::class) constructor(
    filePath: String,
    errorReporter: NonFatalErrorReporter = NonFatalErrorReporter.NONE
) : AbstractDocument(filePath, errorReporter) {

    inner class PdfPage(pageNum: Int) : AbstractPage(pageNum) {
        @Volatile
        private var page: Page? = null
        @Volatile
        private var displayList: DisplayList? = null

        @Volatile
        private var pageTextBuilder: PageTextBuilder? = null

        private fun readPageDataIfNeeded() {
            if (destroyed) return errorInDebug("Page $pageNum already destroyed")
            if (page == null && loadError == null) {
                synchronized(core) {
                    if (page == null && loadError == null) {
                        try {
                            traceTiming({ "Page extraction: $pageNum" }) {
                                page = core.doc.loadPage(pageNum)
                            }
                        } catch (e: IllegalArgumentException) {
                            /* A bad index is an FZ_ERROR_ARGUMENT; its wording differs between mupdf
                               versions, so the page and the count are appended rather than matched. */
                            throw IllegalArgumentException("${e.message}: page $pageNum of ${this@PdfDocument.pageCount}", e)
                        } catch (e: RuntimeException) {
                            /* For comics and standalone images loading the page decodes its image,
                               so a broken or unsupported one ("unknown image file format", a tiff
                               without strips) fails here, and on every retry. The page is marked
                               once and stays blank, instead of throwing again from each caller:
                               rendering, text selection on the UI thread, search, link following. */
                            log("Can't load page $pageNum", e)
                            loadError = e.message ?: e.javaClass.name
                            reportNonFatalOnce("Page ${pageNum + 1} of a .${filePath.substringAfterLast('.', "").lowercase()} book can't be loaded", e)
                        }
                    }
                }
            }
        }

        override fun readPageSize(): PageSize? {
            readPageDataIfNeeded()
            /* A broken page gets the stub size from the caller; any other missing page is a bug. */
            if (loadError != null) return null
            val bbox = page?.bounds ?: errorInDebugOr("Problem extracting page dimension") { return null }
            val pageWidth = bbox.x1 - bbox.x0
            val pageHeight = bbox.y1 - bbox.y0
            return PageSize(pageWidth.toInt(), pageHeight.toInt())
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
            if (displayList == null) return
            val dev: Device =
                AndroidDrawDevice(bitmap, leftOffset, topOffset, left, top, right, bottom)
            try {
                val zoom1 = zoom.toFloat()
                displayList!!.run(dev, Matrix(zoom1, zoom1), null)
                updateContrast(
                    bitmap,
                    top,
                    left,
                    bottom,
                    right,
                    bitmap.width
                )
                dev.close()
            } finally {
                dev.destroy()
            }
        }

        private fun getOrCreateDisplayList() {
            if (displayList != null) return
            synchronized(core) {
                if (displayList == null) {
                    displayList = page?.toDisplayList()
                }
            }
        }

        override fun readPageDataForRendering() {
            readPageDataIfNeeded()
            getOrCreateDisplayList()
        }

        override fun destroyInternal() {
            if (displayList != null) {
                displayList?.destroy()
                displayList = null
            }
            if (page != null) {
                page?.destroy()
                page = null
            }
        }

        override fun searchText(text: String): Array<RectF>? {
            readPageDataIfNeeded()
            if (loadError != null) return null

            return (page ?: errorInDebugOr("No page") {return null}).let {
                core.searchPage(page, text)?.map { it.toRect().run { RectF(x0, y0, x1, y1) } }?.toTypedArray()
            }
        }

        /**
         * The text comes from the display list, not from the page: mupdf allows one thread at a
         * time on a document and its pages, but a finished display list may be replayed from any
         * thread, so this needs no [core] lock and doesn't parse the content stream again. The
         * list is the one built for rendering, hence it also carries annotation and widget text.
         */
        override fun getPageText(): PageText? {
            if (destroyed) return null
            pageTextBuilder?.let { return it }

            return try {
                readPageDataForRendering()
                displayList?.let { getTextInfo(it) }
            } catch (e: Exception) {
                log("Can't extract text of page $pageNum", e)
                null
            }?.also { pageTextBuilder = it }
        }

        override fun readLinks(): List<PageLink> {
            readPageDataIfNeeded()
            val page = page ?: return emptyList()
            return synchronized(core) { extractLinks(page) }
        }

        override fun destroy() {
            destroyPage(this)
        }
    }


    init {
        /* Before the core: opening it is what loads libmupdf_java, see shrinkMupdfStore. */
        mupdfLoaded = true
    }

    private val core = MuPDFCore(filePath)

    /* Called under the core lock: mupdf link loading and destination resolving aren't thread safe. */
    private fun extractLinks(page: Page): List<PageLink> {
        val rawLinks = try {
            page.links
        } catch (e: Exception) {
            log("Can't load links of page ${page.hashCode()}", e)
            null
        } ?: return emptyList()

        return rawLinks.mapNotNull { link ->
            try {
                toPageLink(link)
            } catch (e: Exception) {
                log("Broken link $link", e)
                null
            } finally {
                link.destroy()
            }
        }
    }

    private fun toPageLink(link: Link): PageLink? {
        val uri = link.uri?.takeIf { it.isNotEmpty() } ?: return null
        val bounds = link.bounds
        val target = if (Link.isExternal(uri)) {
            LinkTarget.External(uri)
        } else {
            val dest = core.doc.resolveLinkDestination(uri) ?: return null
            val pageNum = core.doc.pageNumberFromLocation(dest)
            if (pageNum < 0 || pageNum >= pageCount) return null
            LinkTarget.Internal(
                pageNum,
                if (dest.hasX()) dest.x else Float.NaN,
                if (dest.hasY()) dest.y else Float.NaN
            )
        }
        return PageLink(bounds.x0, bounds.y0, bounds.x1, bounds.y1, target)
    }

    override val pageCount: Int
        get() = core.countPages()

    override fun createPage(pageNum: Int): AbstractPage {
        return PdfPage(pageNum)
    }

    override fun destroy() = core.onDestroy()

    override val title: String? by lazy {
        core.title
    }

    /**
     * What mupdf sees of the page tree. It takes the page count from /Root/Pages/Count as is, so a
     * missing or wrong count, a lost catalog or pages node all give zero pages although the file
     * has some; the leaves are counted here by walking /Kids to tell these apart. The header
     * tells an FDF (form data, rightly without pages) or a file that isn't a pdf at all.
     */
    override fun describeStructure(): String = try {
        synchronized(core) {
            val doc = core.doc
            val parts = mutableListOf(describeFileHeader(filePath), "format=${doc.getMetaData(FitzDocument.META_FORMAT)}")
            val pdf = doc.asPDF()
            if (pdf == null) {
                parts += "pdf=false"
            } else {
                val root = pdf.trailer.get("Root")
                val pages = root.get("Pages")
                val kids = pages.get("Kids")
                parts += listOf(
                    "repaired=${pdf.wasRepaired()}",
                    "objects=${pdf.countObjects()}",
                    "root=${root.describeType()}",
                    "pages=${pages.describeType()}",
                    "count=${pages.get("Count").let { if (it.isNull) "missing" else it.toString(true, true).take(20) }}",
                    "kids=${if (kids.isArray) kids.size().toString() else kids.describeType()}",
                    "leaves=${countLeaves(pages)}",
                    "producer=${doc.getMetaData(FitzDocument.META_INFO_PRODUCER).orEmpty().take(60)}",
                    "creator=${doc.getMetaData(FitzDocument.META_INFO_CREATOR).orEmpty().take(60)}",
                )
            }
            parts.joinToString(", ")
        }
    } catch (e: Exception) {
        "structure unavailable: ${e.message}"
    }

    private fun PDFObject.describeType(): String = when {
        isNull -> "missing"
        isDictionary -> get("Type").let { if (it.isName) "/" + it.asName() else "untyped" }
        /* A reference to an absent object ends up here too, as "9 0 R". */
        else -> "not a dictionary: " + toString(true, true).take(20)
    }

    /** Pages as a lenient reader would find them: every node without /Kids below the root is one. */
    private fun countLeaves(node: PDFObject, visited: MutableSet<Int> = HashSet(), depth: Int = 0): Int {
        if (node.isNull || depth > MAX_PAGE_TREE_DEPTH) return 0
        if (node.isIndirect && !visited.add(node.asIndirect())) return 0
        val kids = node.get("Kids")
        if (!kids.isArray) return if (depth > 0) 1 else 0
        var leaves = 0
        for (i in 0 until kids.size()) {
            leaves += countLeaves(kids.get(i), visited, depth + 1)
            if (leaves > MAX_COUNTED_LEAVES) break
        }
        return leaves
    }

    external override fun setContrast(contrast: Int)
    external fun updateContrast(bitmap: Bitmap, startRow: Int, startCol: Int, endRow: Int, endCol: Int, width: Int)

    external override fun setThreshold(threshold: Int)

    private fun getTextInfo(displayList: DisplayList): PageTextBuilder {
        val text: StructuredText = displayList.toStructuredText()
        try {
            return buildTextInfo(text)
        } finally {
            text.destroy()
        }
    }

    private fun buildTextInfo(text: StructuredText): PageTextBuilder {
        val pageTextBuilder = PageTextBuilder()//TODO: cache
        for (block in text.blocks) {
            if (block != null) {
                for (ln in block.lines) {
                    pageTextBuilder.newLine()
                    if (ln != null) {
                        var word = TextWord()
                        pageTextBuilder.addSpace()
                        for (textChar in ln.chars) {
                            if (textChar.c != ' '.code) {
                                word.add(textChar)
                            } else if (word.isNotEmpty()) {
                                if (word.isNotEmpty()) {
                                    pageTextBuilder.addWord(word.toString(), word.rect.toRect())
                                    pageTextBuilder.addSpace()
                                }
                                word = TextWord()
                            }
                        }
                        if (word.isNotEmpty()) {
                            pageTextBuilder.addWord(word.toString(), word.rect.toRect())
                        }
                    }
                }
            }
        }

        return pageTextBuilder
    }

    override val outline: Array<OutlineItem>?
        get() {
            fun collectItems(list: MutableList<OutlineItem>, items: Array<Outline>, level: Int) {
                items.forEach {
                    list.add(OutlineItem(level, it.title ?: "<Empty>", core.pageNumberFromOutline(it)))
                    if (it.down != null) {
                        collectItems(list, it.down, level + 1)
                    }
                }
            }
            return core.outline?.takeIf { it.isNotEmpty() }?.let { items ->
                ArrayList<OutlineItem>(items.size).apply { collectItems(this, items, 0) }.toTypedArray()
            }

        }

    override fun needPassword() = core.needsPassword()

    override fun authenticate(password: String) = core.authenticatePassword(password)

    override val cacheLimit: Long
        get() = MUPDF_STORE_LIMIT

    /* The mupdf store is shared by every open document, so this trims it for all of them. */
    override fun trimCache(keepPercent: Int) = shrinkMupdfStore(keepPercent.coerceIn(0, 100), "trim")

    companion object {
        /* The store is sized once, from FZ_JAVA_STORE_SIZE read when the mupdf context is created;
         * the application sets it before the first mupdf call, the build-time default is the fallback. */
        val MUPDF_STORE_LIMIT: Long = System.getenv("FZ_JAVA_STORE_SIZE")?.toLongOrNull() ?: calcFZCacheSize(0)

        private const val MAX_PAGE_TREE_DEPTH = 64

        private const val MAX_COUNTED_LEAVES = 100_000

    }
}