package documents

import djvu.*
import kotlinx.cinterop.*
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.timing

class DjvuDocument(private val fileName: String) : Document {

    override val pageCount: Int
    private var docPointer: Long = 0
    private var contextRef: Long = 0
    private var lastPagePointer: Long = 0
    private var lastPage = -1

    init {
        contextRef = djvu_DjvuDocument_initContext(null, null)
        println("Context: $contextRef")

        pageCount = memScoped {
            val pageCount1 = this.alloc<LongVar>()
            docPointer = djvu_DjvuDocument_openFile(null, null, fileName.cstr.getPointer(this), pageCount1.ptr, contextRef)
            println(
                "Book: $docPointer size: ${pageCount1.value}"
            )
            pageCount1.value.toInt()
        }
    }

    override fun setThreshold(threshold: Int) {

    }

    override fun setContrast(contrast: Int) {

    }

    override val title: String?
        get() = "TODO"
    override val outline: Array<OutlineItem>
        get() = emptyArray()

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo {
        val result = djvu_DjvuDocument_getPageInfo(null, null, contextRef, docPointer, pageNum, staticCFunction { pN: Int, w: Int, h: Int ->
            val asCPointer: COpaquePointer? = StableRef.create(PageInfo(pN, w, h, null)).asCPointer()
            asCPointer

        })

        val asStableRef = result?.asStableRef<PageInfo>()
        val res = asStableRef?.get() ?: PageInfo(0, 0, 0, null)
        asStableRef?.dispose()

        return res
    }


    private fun gotoPage(page: Int): Long {
        if (lastPage != page) {
            timing("Changing page...") {
                releasePage()
                lastPagePointer = djvu_DjvuDocument_gotoPageInternal(
                    null, null, docPointer,
                    when {
                        page > pageCount - 1 -> pageCount - 1
                        page < 0 -> 0
                        else -> page
                    }
                )
                lastPage = page
            }
        }
        return lastPagePointer
    }

    private fun releasePage() {
        djvu_DjvuDocument_releasePage(null, null, lastPagePointer)
        lastPagePointer = 0
    }


    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        val pagePointer = gotoPage(pageNumber)

        timing("Page $pageNumber rendering") {
            djvu_DjvuDocument_drawPage(
                null,
                null,
                docPointer,
                pagePointer,
                bitmap.pixels.addressOf(0),
                zoom.toFloat(),
                right - left,
                bottom - top,
                left,
                top,
                right - left,
                bottom - top
            )

        }

        println("Total!:" + bitmap.pixels.get().sum());
    }

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
        releasePage()
        djvu_DjvuDocument_destroying(null, null, docPointer, contextRef)
    }

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}