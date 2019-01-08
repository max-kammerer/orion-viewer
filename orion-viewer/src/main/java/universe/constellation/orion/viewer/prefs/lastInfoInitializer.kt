package universe.constellation.orion.viewer.prefs

import universe.constellation.orion.viewer.LastPageInfo

fun initalizer(options: GlobalOptions): LastPageInfo.() -> Unit =
    {
        zoom = options.defaultZoom
        contrast = options.defaultContrast
        walkOrder = options.walkOrder
        pageLayout = options.pageLayout
        colorMode = options.colorMode
    }