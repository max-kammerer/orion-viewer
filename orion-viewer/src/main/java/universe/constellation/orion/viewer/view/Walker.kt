package universe.constellation.orion.viewer.view

import android.graphics.Rect

suspend fun PageView.precache() {
    if (this.state != PageState.SIZE_AND_BITMAP_CREATED) return

    val visibleRect = this.visibleRect() ?: return
    val top = visibleRect.top
    val bottom = visibleRect.bottom
    val left = visibleRect.left
    val right = visibleRect.right
    println("Precaching $pageNum: $visibleRect")
    val sceneInfo = pageLayoutManager.sceneRect

    val deltaX = sceneInfo.width().upperHalf / 2
    val deltaY = sceneInfo.height().upperHalf / 2
    val t = Rect(left - deltaX, top - deltaY, right + deltaX, top)
    renderInvisible(t, "top")
    val b = Rect(left - deltaX, bottom, right + deltaX, bottom + deltaY)
    renderInvisible(b, "bottom")
    val l = Rect(left - deltaX, top, left, bottom)
    renderInvisible(l, "left")
    val r = Rect(right, top, right + deltaX, bottom)
    renderInvisible(r, "right")

    val next = this.pageLayoutManager.uploadNextPage(this, addIfAbsent = true)
    val prev = this.pageLayoutManager.uploadPrevPage(this, addIfAbsent = true)
    val tmp = Rect()

    if (layoutData.globalRect(tmp).bottom < sceneInfo.bottom + 1.5 * deltaY) {
        //TODO add global walk
        next?.precacheData()
    }

    if (layoutData.globalRect(tmp).top >= -2 * deltaY) {
        this.layoutData.globalRect(tmp).bottom
        prev?.precacheData()
    }
}

fun Rect.screenForPrecache(pageLayoutManager: PageLayoutManager) {
    val sceneInfo = pageLayoutManager.sceneRect
    set(pageLayoutManager.sceneRect)
    inset(-sceneInfo.width().upperHalf / 2, -sceneInfo.height().upperHalf / 2)
}

internal val Int.upperHalf
    get(): Int {
        return this / 2 + this % 2
    }