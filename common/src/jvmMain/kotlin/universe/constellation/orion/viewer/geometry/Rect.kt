package universe.constellation.orion.viewer.geometry

/* Plain counterparts of android.graphics.Rect/RectF with the subset the common code uses. */
actual class Rect actual constructor(var left: Int, var top: Int, var right: Int, var bottom: Int) {
    actual constructor() : this(0, 0, 0, 0)

    actual fun width(): Int = right - left
    actual fun height(): Int = bottom - top
    fun isEmpty(): Boolean = left >= right || top >= bottom

    /** Same rules as Android: an empty rectangle takes the other one, otherwise the union grows. */
    actual fun union(r: Rect) {
        if (r.left >= r.right || r.top >= r.bottom) return
        if (isEmpty()) {
            left = r.left; top = r.top; right = r.right; bottom = r.bottom
        } else {
            if (left > r.left) left = r.left
            if (top > r.top) top = r.top
            if (right < r.right) right = r.right
            if (bottom < r.bottom) bottom = r.bottom
        }
    }

    override fun equals(other: Any?): Boolean =
        other is Rect && left == other.left && top == other.top && right == other.right && bottom == other.bottom

    override fun hashCode(): Int = ((left * 31 + top) * 31 + right) * 31 + bottom

    override fun toString(): String = "Rect($left, $top - $right, $bottom)"
}

actual class RectF(var left: Float, var top: Float, var right: Float, var bottom: Float) {
    constructor() : this(0f, 0f, 0f, 0f)

    override fun toString(): String = "RectF($left, $top - $right, $bottom)"
}
