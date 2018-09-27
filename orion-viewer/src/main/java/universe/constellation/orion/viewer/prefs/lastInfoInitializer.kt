package universe.constellation.orion.viewer.prefs

import universe.constellation.orion.viewer.LastPageInfo

fun initalizer(options: GlobalOptions): LastPageInfo.() -> Unit =
    {
        zoom = options.getDefaultZoom()
        contrast = options.getDefaultContrast()
        walkOrder = options.getWalkOrder()
        pageLayout = options.getPageLayout()
        colorMode = options.getColorMode()
    }