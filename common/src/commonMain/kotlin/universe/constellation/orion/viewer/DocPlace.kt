package universe.constellation.orion.viewer

import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.OneDimension
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A place in the document: a page plus the viewport offset as a fraction of the *whole* page,
 * crop margins included.
 *
 * Fractions survive zoom and crop changes; pixel offsets (see [LayoutPosition]) don't, so this
 * is the only position type navigation code passes around.
 * TODO: the fractions are in rotated coordinates, so a rotation change between leaving and
 *  returning swaps the axes.
 */
data class DocPlace(val page: Int, val xFraction: Float = 0f, val yFraction: Float = 0f) {

    fun isSamePlace(other: DocPlace): Boolean =
        page == other.page &&
                abs(xFraction - other.xFraction) < SAME_PLACE_TOLERANCE &&
                abs(yFraction - other.yFraction) < SAME_PLACE_TOLERANCE

    fun serialize() = "$page:$xFraction:$yFraction"

    companion object {
        private const val SAME_PLACE_TOLERANCE = 0.01f

        fun parse(text: String): DocPlace? {
            val parts = text.split(":")
            if (parts.size != 3) return null
            return DocPlace(
                parts[0].toIntOrNull() ?: return null,
                parts[1].toFloatOrNull() ?: return null,
                parts[2].toFloatOrNull() ?: return null
            )
        }
    }
}

/** Why the viewer moves: decides whether the place being left is remembered for "back". */
enum class NavKind {
    /** Explicit jump (outline, goto, bookmark): the place left goes into the history. */
    JUMP,
    /**
     * One step of a series (±10 pages, search hits): only the place the series started from goes
     * into the history. A series continues while the user hasn't moved away from where the
     * previous step landed.
     */
    STEP,
    /** Back/forward and paging: the history is untouched. */
    PAGING
}

fun LayoutPosition.toDocPlace() = DocPlace(pageNumber, x.fraction(), y.fraction())

/** Position of the viewport start on the whole (uncropped) page, 0..1. */
private fun OneDimension.fraction(): Float {
    val full = wholeDimension
    return if (full > 0) ((marginLeft + offset).toFloat() / full).coerceIn(0f, 1f) else 0f
}

/** Inverse of [fraction]: the offset inside the cropped area that puts [fraction] of the whole page at the viewport start. */
fun OneDimension.offsetAtFraction(fraction: Float): Int =
    (fraction * wholeDimension - marginLeft).roundToInt().coerceAtLeast(0)

private val OneDimension.wholeDimension: Int
    get() = marginLeft + pageDimension + marginRight
