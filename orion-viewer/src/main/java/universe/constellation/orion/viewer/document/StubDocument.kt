package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.Bitmap
import universe.constellation.orion.viewer.PageDimension
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.geometry.RectF
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy

class StubDocument(pathOrMessage: String, var bodyText: String = pathOrMessage) : AbstractDocument("Stub[$pathOrMessage]") {

    override var title: String = pathOrMessage.substringAfterLast("/")

    override fun createPage(pageNum: Int): PageWithAutoCrop {
        return object: PageWithAutoCrop(pageNum) {
            override fun getPageDimension(): PageDimension = PageDimension(300, 400)

            override fun getPageInfo(
                layoutStrategy: SimpleLayoutStrategy
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

            override fun destroyInternal() {}

            override fun searchText(text: String): Array<RectF>? {
                return null
            }

            override fun getText(
                absoluteX: Int,
                absoluteY: Int,
                width: Int,
                height: Int,
                singleWord: Boolean
            ): String? {
                return null
            }
        }
    }

    override fun setThreshold(threshold: Int) {}

    override fun setContrast(contrast: Int) {}

    override val pageCount: Int
        get() = 1

    override val outline: Array<OutlineItem> = emptyArray()

    override fun destroy() {}
}