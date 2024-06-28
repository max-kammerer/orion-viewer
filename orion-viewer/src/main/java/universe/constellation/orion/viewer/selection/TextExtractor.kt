package universe.constellation.orion.viewer.selection

import android.graphics.Rect
import android.graphics.RectF
import universe.constellation.orion.viewer.document.TextAndSelection
import universe.constellation.orion.viewer.document.TextInfoBuilder
import universe.constellation.orion.viewer.document.TextWord
import kotlin.math.max
import kotlin.math.min

private fun getTextByHandlers(builder: TextInfoBuilder, startHandler: Handler, endHandler: Handler): TextAndSelection? {
    val opRect = RectF()
    val lns: java.util.ArrayList<List<TextWord>> = arrayListOf()
    val selectionRegion = RectF(
        min(startHandler.x, endHandler.x),
        min(startHandler.y, endHandler.y),
        max(startHandler.x, endHandler.x),
        max(startHandler.y, endHandler.y)
    )

    val selectionRegionFull = RectF(
        0f,
        min(startHandler.y, endHandler.y),
        max(startHandler.x, endHandler.x),
        Float.MAX_VALUE
    )

    for (line in builder.lines) {
        val wordsInLine = arrayListOf<TextWord>()
        for (word in line) {
            if (word.isNotEmpty()) {
                processWord(word, wordsInLine, selectionRegion, false, opRect)
            }
        }

        if (wordsInLine.size > 0) {
            lns.add(wordsInLine)
        }
    }
    if (lns.isEmpty()) return null

    if (startHandler.y >= endHandler.y) {
        lns.first() to lns.last()
    } else {
        lns.last() to lns.first()
    }

    val result = foldResults(lns)

    return if (result.isNotEmpty()) {
        TextAndSelection(result.toString(), RectF(result.rect), builder);
    } else {
        null
    }
}


private fun getText(builder: TextInfoBuilder, absoluteX: Int, absoluteY: Int, width: Int, height: Int, singleWord: Boolean): TextAndSelection? {
    val opRect = RectF()
    val lns: java.util.ArrayList<List<TextWord>> = arrayListOf()
    val selectionRegion = RectF(
        absoluteX.toFloat(),
        absoluteY.toFloat(),
        (absoluteX + width).toFloat(),
        (absoluteY + height).toFloat()
    )

    for (line in builder.lines) {
        val wordsInLine = arrayListOf<TextWord>()
        for (word in line) {
            if (word.isNotEmpty()) {
                processWord(word, wordsInLine, selectionRegion, singleWord, opRect)
            }
        }

        if (wordsInLine.size > 0) {
            lns.add(wordsInLine)
        }
    }

    val result = foldResults(lns)

    return if (result.isNotEmpty()) {
        TextAndSelection(result.toString(), RectF(result.rect), builder);
    } else {
        null
    }
}

private fun foldResults(lns: ArrayList<List<TextWord>>): TextWord {
    val result = TextWord()
    lns.fold(result) { acc, line ->
        if (acc.isNotEmpty()) acc.add(" ", Rect())

        val lineRes = TextWord()
        val res = line.fold(lineRes) { accLine, tc ->
            if (accLine.isNotEmpty()) accLine.add(" ", Rect())
            accLine.add(tc.toString(), tc.rect)
            accLine
        }
        acc.add(res.toString(), res.rect)
        acc
    }
    return result
}

private fun processWord(
    word: TextWord,
    words: java.util.ArrayList<TextWord>,
    region: RectF,
    isSingleWord: Boolean,
    opRect: RectF
) {
    val wordSquare5: Float = word.width() * word.height() / 5f
    opRect.set(word.rect)
    if (opRect.intersect(region)) {
        if (isSingleWord || opRect.width() * opRect.height() > wordSquare5) {
            words.add(word)
        }
    }
}