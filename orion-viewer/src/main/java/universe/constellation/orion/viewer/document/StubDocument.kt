package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.geometry.RectF

class StubDocument(override var title: String?, var bodyText: String? = title) : DocumentWithCaching {

    override fun setThreshold(threshold: Int) {}

    override fun setContrast(contrast: Int) {}

    override val pageCount: Int
        get() = 1

    override val outline: Array<OutlineItem> = emptyArray()

    override fun getPageInfo(pageNum: Int) =
        PageInfo(0, 100, 100)

    override fun getPageInfo(pageNum: Int, cropMode: Int): PageInfo {
        return getPageInfo(pageNum)
    }

    override fun renderPage(pageNumber: Int, bitmap: Bitmap, zoom: Double, left: Int, top: Int, right: Int, bottom: Int, lefCrop: Int, topCrop: Int) {
        //TODO: render body text
    }

    override fun getText(pageNumber: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? =
            null

    override fun destroy() {}

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? = null

    override fun toString(): String {
        return "Stub[$title]"
    }
}