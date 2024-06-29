package universe.constellation.orion.viewer.document

import android.graphics.RectF

data class TextAndSelection(val value: String, val rect: List<RectF>, val textInfo: TextInfoBuilder) {
    constructor(value: String, rect: RectF, textInfo: TextInfoBuilder) : this(value, listOf(rect), textInfo)
}