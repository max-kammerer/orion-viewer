package universe.constellation.orion.viewer.view

import android.graphics.Rect
import android.graphics.Region
import universe.constellation.orion.viewer.PageState
import universe.constellation.orion.viewer.PageView

suspend fun PageView.precache() {
    if (this.state != PageState.SIZE_AND_BITMAP_CREATED) return
    if (nonRenderedRegion.isEmpty) return

    val visibleRect = this.visibleRect() ?: return
    val top = visibleRect.top
    val bottom = visibleRect.bottom
    val left = visibleRect.left
    val right = visibleRect.right
    val width = visibleRect.width()
    val height = visibleRect.height()


    this.tempRegion.set(nonRenderedRegion)
    if (tempRegion.op(visibleRect, Region.Op.INTERSECT)) return //UI will trigger necessary event

    this.tempRegion.set(nonRenderedRegion)

    val deltaX = width/3
    val deltaY = height/3
    val t = Rect(left - deltaX, top - deltaY, right + deltaX, top)
    renderInvisible(t)
    val b = Rect(left - deltaX, bottom, right + deltaX, bottom + deltaY)
    renderInvisible(b)
    val l = Rect(left - deltaX, top, left, bottom)
    renderInvisible(l)
    val r = Rect(right, top, right + deltaX, bottom)
    renderInvisible(r)

    this.pageLayoutManager.uploadNextPage(this, true)
    this.pageLayoutManager.uploadPrevPage(this, true)
}
