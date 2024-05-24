package universe.constellation.orion.viewer

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

class LayoutData {
    val position: PointF = PointF(0f, 0f)
    val wholePageRect: Rect = Rect()

    val globalLeft: Float
        get() = position.x + wholePageRect.left

    val globalRight: Float
        get() = position.x + wholePageRect.right

    val globalBottom: Float
        get() = position.y + wholePageRect.bottom


    fun contains(x: Float, y: Float): Boolean {
        return wholePageRect.contains((x - position.x).toInt(), (y - position.y).toInt())
    }

    fun containsY(y: Float): Boolean {
        return wholePageRect.contains(wholePageRect.left, (y - position.y).toInt())
    }

    fun globalRect(target: Rect): Rect {
        target.set(wholePageRect)
        target.offset(position.x.toInt(), position.y.toInt())
        return target
    }

    fun globalRect(target: RectF): RectF {
        target.set(wholePageRect)
        target.offset(position.x, position.y)
        return target
    }

    fun pagePartOnScreen(screenRect: Rect, target: Rect): Rect? {
        val globalPosition = globalRect(target)
        return if (globalPosition.intersect(screenRect)) {
            globalPosition
        } else {
            null
        }
    }

    fun visibleOnScreenPart(screenRect: Rect): Rect? {
        val occupiedScreenPart = pagePartOnScreen(screenRect, Rect()) ?: return null
        occupiedScreenPart.offset(-position.x.toInt(), -position.y.toInt())
        return occupiedScreenPart
    }

    fun toLocalCoord(screenRect: Rect): Rect {
        screenRect.offset(-position.x.toInt(), -position.y.toInt())
        return screenRect
    }

    override fun toString(): String {
        return "LayoutData(position=$position, viewDimension=$wholePageRect)"
    }

}