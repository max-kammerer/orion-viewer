package universe.constellation.orion.viewer.selection

import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toRectF
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.document.TextAndSelection
import universe.constellation.orion.viewer.document.TextInfoBuilder
import universe.constellation.orion.viewer.document.TextWord

class ExtractionInfo(val page: Page, val absoluteRectWithoutCrop: RectF, val sceneRect: (RectF) -> RectF)

fun SelectionAutomata.getTextByHandlers(startHandler: Handler, endHandler: Handler, isRect: Boolean = false, isSingleWord: Boolean = false): TextAndSelection? {
    val screenSelection = SelectionAutomata.getScreenSelectionRect(startHandler, endHandler)
    val pageLayoutManager = this.activity.controller!!.pageLayoutManager
    val pageSelectionRectangles = getPageSelectionRectangles(
        screenSelection,
        false,
        pageLayoutManager
    )
    val invertCheck = startHandler.y > endHandler.y

    return extractText(pageSelectionRectangles.map { ExtractionInfo(it.page, it.absoluteRectWithoutCrop.toRectF(), it.pageView::getSceneRect) }, isRect, isSingleWord, invertCheck)
}



fun extractText(
    extractionInfo: List<ExtractionInfo>,
    isRectMode: Boolean,
    isSingleWord: Boolean,
    invertCheck: Boolean
): TextAndSelection? {
    val opRect = RectF()
    val lns: java.util.ArrayList<List<TextWord>> = arrayListOf()
    val rects: ArrayList<RectF> = arrayListOf()

    for (selection in extractionInfo) {
        for (line in selection.page.getTextInfo()?.lines ?: emptyList()) {
            val wordsInLine = arrayListOf<TextWord>()
            val region = selection.absoluteRectWithoutCrop
            for (word in line) {
                if (word.isNotEmpty()) {
                    if (isRectMode || isSingleWord) {
                        if (includeWord(word, region, isSingleWord, opRect)) {
                            wordsInLine.add(word)
                            rects.add(selection.sceneRect(word.rect.toRectF()))
                        }
                    } else {
                        if (includeWordComplex(word, region, invertCheck)) {
                            wordsInLine.add(word)
                            rects.add(selection.sceneRect(word.rect.toRectF()))
                        }
                    }
                }
            }

            if (wordsInLine.size > 0) {
                lns.add(wordsInLine)
            }
        }
    }

    if (lns.isEmpty()) return null

    val result = foldResults(lns)

    return if (result.isNotEmpty()) {
        TextAndSelection(result.toString(), rects, TextInfoBuilder())
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

private fun includeWord(
    word: TextWord,
    region: RectF,
    isSingleWord: Boolean,
    opRect: RectF
): Boolean {
    val wordSquare3: Float = word.width() * word.height() / 4f
    opRect.set(word.rect)
    if (opRect.intersect(region)) {
        if (isSingleWord || opRect.width() * opRect.height() > wordSquare3) {
            return true
        }
    }
    return false
}

private fun includeWordComplex(
    textWord: TextWord,
    selection: RectF,
    invertCheck: Boolean
): Boolean {
    val rect = textWord.rect
    val top = selection.top
    val bottom = selection.bottom
    if (top < rect.top && bottom > rect.bottom) {
        return true
    }

    val top2 = selection.top
    val bottom2 = selection.bottom

    var insideTop  = false
    var topCheck  = false
    var insideBottom  = false
    var bottomCheck  = false

    if (rect.top <= top2 && top2 <= rect.bottom) {
        if (rect.bottom - top2 >= rect.height() / 4) {
            insideTop = true
            val left = if (false && invertCheck) selection.right else selection.left
            if (rect.left + rect.width() / 2 >= left) topCheck = true
        }
    }

    if (rect.top <= bottom2 && bottom2 <= rect.bottom) {
        if (bottom2 - rect.top >= rect.height() / 4) {
            insideBottom = true
            val right = if (false && invertCheck) selection.left else selection.right
            if (rect.left + rect.width() / 2 <= right) bottomCheck = true
        }
    }

    if (insideTop && insideBottom) {
        return topCheck && bottomCheck
    }
    if (insideTop) return topCheck
    if (insideBottom) return bottomCheck


    return false
}