package universe.constellation.orion.viewer.rendering

import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.OneDimension

/**
 * Created by mike on 27.10.15.
 */

fun LayoutPosition.insideWidth() = x.pageDimension <= x.screenDimension

fun OneDimension.visibeSpaceAfter() = Math.max(0, screenDimension - (screenOffset + pageDimension))

val OneDimension.screenOffset: Int
    get() = -offset

public fun LayoutPosition.isContiniousMode(): Boolean {
    //*todo check context*/
    return insideWidth()
}