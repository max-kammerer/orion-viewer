package universe.constellation.orion.viewer.layout

actual class State(
    actual var screenWidth: Int,
    actual var screenHeight: Int,
    actual var pageNumber: Int,
    actual var rotation: Int,
    actual var screenOrientation: String,
    actual var newOffsetX: Int,
    actual var newOffsetY: Int,
    actual var zoom: Int,
    actual var leftMargin: Int,
    actual var rightMargin: Int,
    actual var topMargin: Int,
    actual var bottomMargin: Int,
    actual var enableEvenCropping: Boolean,
    actual var cropMode: Int,
    actual var leftEvenMargin: Int,
    actual var rightEventMargin: Int,
    actual var pageLayout: Int,
    actual var contrast: Int,
    actual var threshold: Int,
    actual var walkOrder: String,
    actual var colorMode: String
)