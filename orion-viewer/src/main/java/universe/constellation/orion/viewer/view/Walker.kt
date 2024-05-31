package universe.constellation.orion.viewer.view

import android.graphics.Rect
import universe.constellation.orion.viewer.log

fun PageView.precache() {
    log("Precaching $pageNum: $state")
    if (this.state != PageState.SIZE_AND_BITMAP_CREATED) return

    val globalPageRect = this.layoutData.globalRect(Rect())
    log("Precaching $pageNum: $globalPageRect")

    val sceneInfo = pageLayoutManager.sceneRect

    val topY = sceneInfo.top
    val bottomY = sceneInfo.bottom
    val leftX = sceneInfo.left
    val rightX = sceneInfo.right
    val deltaX = deltaX(sceneInfo)
    val deltaY = getDeltaY(sceneInfo)

    val top = Rect(leftX - deltaX, topY - deltaY, rightX + deltaX, topY)
    precacheSide(top, globalPageRect, "top")
    val bottom = Rect(leftX - deltaX, bottomY, rightX + deltaX, bottomY + deltaY)
    precacheSide(bottom, globalPageRect, "bottom")
    val left = Rect(leftX - deltaX, topY, leftX, bottomY)
    precacheSide(left, globalPageRect, "left")
    val right = Rect(rightX, topY, rightX + deltaX, bottomY)
    precacheSide(right, globalPageRect, "right")
}

fun PageView.precacheNeighbours(next: Boolean) {
    val isCurrentPageInitialized = this.state != PageState.SIZE_AND_BITMAP_CREATED
    log("Precaching Neighbours $pageNum")
    val sceneInfo = pageLayoutManager.sceneRect

    val tmp = this.layoutData.globalRect(Rect())

    val deltaY = getDeltaY(sceneInfo)

    if (next && layoutData.globalRect(tmp).bottom < sceneInfo.bottom + deltaY) {
        pageLayoutManager.uploadNextPage(this, addIfAbsent = true)?.takeIf { isCurrentPageInitialized }?.precacheData()
    }

    if (!next && layoutData.globalRect(tmp).top >= -deltaY) {
        this.layoutData.globalRect(tmp).bottom
        pageLayoutManager.uploadPrevPage(this, addIfAbsent = true)?.takeIf { isCurrentPageInitialized }?.precacheData()
    }
}

fun PageView.precacheSide(rectOnScreen: Rect, pageGlobal: Rect, side: String) {
    val intersect = rectOnScreen.intersect(pageGlobal)
    if (intersect && !rectOnScreen.isEmpty) {
        println("precache $pageNum: $side")
        renderInvisible(layoutData.toLocalCoord(rectOnScreen), side)
    }
}

fun Rect.activeScreenArea(pageLayoutManager: PageLayoutManager) {
    val sceneInfo = pageLayoutManager.sceneRect
    set(pageLayoutManager.sceneRect)
    inset(-deltaX(sceneInfo) / 2, -getDeltaY(sceneInfo) / 2)
}

private fun getDeltaY(sceneInfo: Rect) = sceneInfo.height().upperHalf

private fun deltaX(sceneInfo: Rect) = sceneInfo.width().upperHalf

internal val Int.upperHalf
    get(): Int {
        return this / 2 + this % 2
    }