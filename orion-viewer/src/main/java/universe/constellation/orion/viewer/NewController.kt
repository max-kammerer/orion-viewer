package universe.constellation.orion.viewer

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import org.jetbrains.anko.uiThread
import universe.constellation.orion.viewer.Common.d
import universe.constellation.orion.viewer.scene.*
import universe.constellation.orion.viewer.view.Renderer

/**
 * Created by mike on 1/2/17.
 */
class NewController(
        val screen: Screen,
        activity: OrionViewerActivity,
        doc: DocumentWrapper,
        val layout: LayoutStrategy,
        renderer: Renderer
) : Controller(activity, doc, layout, renderer, false), PageProvider {

    lateinit var actualPage: LazyPage
    val layoutPosition: LayoutPosition = LayoutPosition()

    override fun drawNext() {
        super.drawNext()
    }

    override fun drawPrev() {
        super.drawPrev()
    }

    override fun drawPage(info: LayoutPosition) {
        actualPage = createLazyPage(info)
        screen.invalidate()
    }

    override fun drawPage() {
        screen.invalidate()
    }

    override fun drawPage(page: Int) {
        layout.reset(layoutPosition, page)
        actualPage = createLazyPage(layoutPosition)
        drawPage()
    }


    override fun init(info: LastPageInfo, dimension: Point) {
        layout.reset(layoutPosition, info.pageNumber)
        actualPage = createLazyPage(layoutPosition)
        super.init(info, dimension)
    }

    fun createLazyPage(info: LayoutPosition) : LazyPage {
        d("create lazy page: $info")
        return LazyPage(info.pageNumber, Position(0, 0), Dimension(info.x.pageDimension, info.y.pageDimension), this, screen).apply {
            screen.getOrCreatePage(this)
        }
    }

    override fun getPageInfo(page: Int, pageInfoConsumer: PageInfoConsumer) {
        d("getPageInfo $page")
        val document = document
        if (document.hasCalculatedPageInfo(page)) {
            pageInfoConsumer.onNewEvent(document.getPageInfo(page, 0 /*TODO support cropping*/))
        }
        else {
            orionAsync {
                val pageInfo = document.getPageInfo(page, 0 /*TODO support cropping*/)
                uiThread {
                    pageInfoConsumer.onNewEvent(pageInfo)
                }
            }
        }
    }

    override fun translateAndZoom(changeZoom: Boolean, zoomScaling: Float, deltaX: Float, deltaY: Float) {
        d("translateAndZoom>")
        super.translateAndZoom(changeZoom, zoomScaling, deltaX, deltaY)
        /*TODO calc proper deltas*/
        screen.onMove(Position(-deltaX.toInt(), -deltaY.toInt()))
    }

    override fun render(page: Int, bitmap: Bitmap, zoom: Double, rect: Rect) {
        checkNonUIThread()
        document.renderPage(page, bitmap, zoom, rect.left, rect.top, rect.right, rect.bottom)
    }
}