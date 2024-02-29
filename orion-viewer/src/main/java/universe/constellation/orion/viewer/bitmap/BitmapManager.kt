package universe.constellation.orion.viewer.bitmap

import android.graphics.Rect
import universe.constellation.orion.viewer.PageView
import universe.constellation.orion.viewer.view.PageLayoutManager

class BitmapManager(val pageLayoutManager: PageLayoutManager) {

    fun createDefaultBitmap(wholePageRect: Rect): FlexibleBitmap {
        val deviceInfo = pageLayoutManager.controller.getDeviceInfo()
        return FlexibleBitmap(wholePageRect, deviceInfo.width / 2, deviceInfo.height / 2)
    }

    fun actualizeActive(pageView: PageView) {
        val bitmap = pageView.bitmap ?: return
        actualizeActive(bitmap, pageView.layoutData.visibleOnScreenPart(pageLayoutManager.sceneRect)?: return)
    }

    private fun actualizeActive(
        bitmap: FlexibleBitmap,
        visiblePart: Rect
    ) {
        visiblePart.inset(-bitmap.partWidth / 2, -bitmap.partHeight / 2)
        bitmap.updateDrawAreaAndUpdateNonRenderingPart(
            visiblePart,
            pageLayoutManager.controller.bitmapCache
        )
    }
}