package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageDimension
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy

class StubDocument(override var title: String?, var bodyText: String? = title) : AbstractDocument() {

    override fun createPage(pageNum: Int): PageWithAutoCrop {
        return object: PageWithAutoCrop(pageNum) {
            override fun getPageDimension(): PageDimension = PageDimension(300, 400)

            override fun getPageInfo(
                layoutStrategy: SimpleLayoutStrategy,
                cropMode: Int
            ): PageInfo {
                val pageDim = getPageDimension()
                return PageInfo(pageNum, pageDim.width, pageDim.height)
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
            }

            override fun readPageDataForRendering() {}

            override fun destroy() {}
        }
    }

    override fun setThreshold(threshold: Int) {}

    override fun setContrast(contrast: Int) {}

    override val pageCount: Int
        get() = 1

    override val outline: Array<OutlineItem> = emptyArray()

    override fun getText(pageNum: Int, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): String? =
            null

    override fun destroy() {}

    override fun searchPage(pageNumber: Int, text: String): Array<RectF>? = null

    override fun toString(): String {
        return "Stub[$title]"
    }
}