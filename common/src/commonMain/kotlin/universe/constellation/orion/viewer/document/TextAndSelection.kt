package universe.constellation.orion.viewer.document

import android.graphics.RectF

data class TextAndSelection(val value: String, val rect: List<RectF>, val textInfo: PageTextBuilder) {
    constructor(value: String, rect: RectF, textInfo: PageTextBuilder) : this(value, listOf(rect), textInfo)
}