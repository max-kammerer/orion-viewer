package universe.constellation.orion.viewer

import android.graphics.PointF
import android.graphics.Rect

class LayoutData {
    val position: PointF = PointF(0f, 0f)
    val wholePageRect: Rect = Rect()
    val tmpRect: Rect = Rect()

    val globalLeft: Float
        get() = position.x + wholePageRect.left

    val globalRight: Float
        get() = position.x + wholePageRect.right

    val globalBottom: Float
        get() = position.y + wholePageRect.bottom


    fun contains(x: Float, y: Float): Boolean {
        return wholePageRect.contains((x - position.x) .toInt(), (y-position.y).toInt())
    }

    fun globalRectInTmp(): Rect {
        tmpRect.set(wholePageRect)
        tmpRect.offset(position.x.toInt(), position.y.toInt())
        return tmpRect
    }

    fun occupiedScreenPartInTmp(screenRect: Rect): Rect? {
        val globalPosition = globalRectInTmp()
        return if (globalPosition.intersect(screenRect)) {
            globalPosition
        } else {
            null
        }
    }

    fun visibleOnScreenPart(screenRect: Rect): Rect? {
        val occupiedScreenPart = occupiedScreenPartInTmp(screenRect) ?: return null
        occupiedScreenPart.offset(-position.x.toInt(), -position.y.toInt())
        return Rect(occupiedScreenPart)
    }

    fun insideScreenX(screenRect: Rect): Boolean {
        return wholePageRect.left >= screenRect.left && wholePageRect.right <= screenRect.right
    }

    override fun toString(): String {
        return "LayoutData(position=$position, viewDimension=$wholePageRect)"
    }

}