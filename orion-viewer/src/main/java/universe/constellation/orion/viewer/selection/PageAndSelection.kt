package universe.constellation.orion.viewer.selection

import android.graphics.Rect
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.view.PageView

class PageAndSelection(val pageView: PageView, val absoluteRectWithoutCrop: Rect) {
    val page: Page
        get() = pageView.page
}