package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.layout.AutoCropMargins

actual data class PageInfo actual constructor(
    actual val pageNum0: Int,
    actual var width: Int,
    actual var height: Int
) {
    actual var autoCrop: AutoCropMargins? = null
}

actual data class PageSize actual constructor(actual var width: Int, actual var height: Int)
