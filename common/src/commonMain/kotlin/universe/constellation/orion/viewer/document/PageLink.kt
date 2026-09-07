package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.DocPlace
import universe.constellation.orion.viewer.PageSize

sealed class LinkTarget {
    /**
     * A place inside the document. [x] and [y] are page coordinates (points, top-left origin)
     * of the destination; NaN when the link points at the page as a whole.
     */
    data class Internal(val page: Int, val x: Float = Float.NaN, val y: Float = Float.NaN) : LinkTarget()

    /** A URI to be opened outside the viewer. */
    data class External(val uri: String) : LinkTarget()
}

/** A clickable area of a page in page coordinates (points, top-left origin, no crop applied). */
class PageLink(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val target: LinkTarget
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    override fun toString(): String = "PageLink([$left, $top, $right, $bottom] -> $target)"
}

fun List<PageLink>.findAt(x: Float, y: Float): PageLink? = firstOrNull { it.contains(x, y) }

/**
 * The place the viewport should be scrolled to: the destination line at the top of the screen.
 * The horizontal position is ignored, a fit-width page has nothing to scroll there anyway.
 */
fun LinkTarget.Internal.toDocPlace(pageSize: PageSize): DocPlace {
    val yFraction = if (!y.isNaN() && pageSize.height > 0) (y / pageSize.height).coerceIn(0f, 1f) else 0f
    return DocPlace(page, 0f, yFraction)
}
