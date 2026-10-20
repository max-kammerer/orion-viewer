package universe.constellation.orion.viewer.prefs

import universe.constellation.orion.viewer.LastPageInfo

fun initalizer(options: GlobalOptions): LastPageInfo.() -> Unit =
    {
        zoom = options.defaultZoom
        contrast = options.defaultContrast
        //the crop mode stays manual, the default for a new book
        leftMargin = options.defaultCropLeft
        rightMargin = options.defaultCropRight
        topMargin = options.defaultCropTop
        bottomMargin = options.defaultCropBottom
        walkOrder = options.walkOrder
        pageLayout = options.pageLayout
        colorMode = options.colorMode
    }