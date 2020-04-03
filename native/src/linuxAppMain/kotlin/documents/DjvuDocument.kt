package documents

import djvu.djvu_DjvuDocument_destroying
import djvu.djvu_DjvuDocument_getPageInfo
import djvu.djvu_DjvuDocument_initContext
import djvu.djvu_DjvuDocument_openFile
import kotlinx.cinterop.*
import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.geometry.RectF

class DjvuDocument(private val fileName: String) : Document {

    override val pageCount: Int
    private var bookRef: Long = 0
    private var contextRef: Long = 0

    init {
        contextRef = djvu_DjvuDocument_initContext(null, null)
        println("Context: $contextRef")

        pageCount = memScoped {
            val pageCount1 = this.alloc<LongVar>()
            bookRef = djvu_DjvuDocument_openFile(null, null, fileName.cstr.getPointer(this), pageCount1.ptr, contextRef)
            println(
                "Book: $bookRef size: ${pageCount1.value}"
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

    override fun getPageInfo(pageNum: Int): PageInfo {
//        djvu_DjvuDocument_openFile()

        val result = djvu_DjvuDocument_getPageInfo(null, null, bookRef, pageNum, staticCFunction { pN: Int, w: Int, h: Int ->
            val asCPointer: COpaquePointer? = StableRef.create(PageInfo(pN, w, h, null)).asCPointer()
            asCPointer

        } )

        println(result)
        val asStableRef = result?.asStableRef<PageInfo>()
        println("foo")
        val res = asStableRef?.get() ?: PageInfo(0, 0, 0, null)
        println("dispose")
        asStableRef?.dispose()

        return res
    }

    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
        djvu_DjvuDocument_destroying(null, null, bookRef, contextRef)
    }

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}