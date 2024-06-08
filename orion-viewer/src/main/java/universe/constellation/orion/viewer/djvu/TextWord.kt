package universe.constellation.orion.viewer.djvu

import android.graphics.Rect

class TextWord {

    private var stringBuilder = StringBuilder()

    val rect = Rect()

    fun add(value: String, rect: Rect) {
        stringBuilder.append(value)
        this.rect.union(rect)
    }

    fun isNotEmpty(): Boolean {
        return stringBuilder.isNotEmpty()
    }

    fun width(): Int {
        return rect.width()
    }

    fun height(): Int {
        return rect.height()
    }

    override fun toString(): String {
        return stringBuilder.toString()
    }
}