package universe.constellation.orion.viewer.djvu

import android.graphics.Rect

class TextBuilder {

    companion object {

        val NULL = TextBuilder()

        val space = TextWord().apply { add(" ", Rect()) }
    }

    val lines = mutableListOf<MutableList<TextWord>>()

    fun addWord(value: String, x: Int, y: Int, x2: Int, y2: Int) {
        lastLine().add(TextWord().apply { add(value, Rect(x, y, x2, y2)) })
    }

    private fun lastLine(): MutableList<TextWord> {
        if (lines.isEmpty()) {
            lines.add(mutableListOf())
        }
        return lines.last()
    }

    fun newLine() {
        lines.add(mutableListOf())
    }

    fun addSpace() {
        lastLine().add(space)
    }
}