package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.layout.AutoCropMargins

actual class PageInfo(
    actual val pageNum0: Int,
    actual var width: Int,
    actual var height: Int,
    actual var autoCrop: AutoCropMargins?
)