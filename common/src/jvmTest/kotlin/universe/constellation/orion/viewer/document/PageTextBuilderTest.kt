package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class PageTextBuilderTest {

    @Test
    fun wordsKeepTheirLinesAndBounds() {
        val builder = PageTextBuilder()
        builder.addWord("Hello", 10, 20, 50, 30)
        builder.addSpace()
        builder.addWord("world", Rect(60, 20, 110, 30))
        builder.newLine()
        builder.newLine() // an empty line is not opened twice
        builder.addWord("next", 10, 40, 40, 50)

        assertEquals(2, builder.lines.size)
        assertEquals(listOf("Hello", " ", "world"), builder.lines[0].map { it.toString() })
        assertEquals(listOf("next"), builder.lines[1].map { it.toString() })
        assertEquals(Rect(10, 20, 50, 30), builder.lines[0][0].rect)
    }

    @Test
    fun wordGrowsWithEveryPiece() {
        val word = TextWord()
        word.add("ab", Rect(0, 0, 10, 10))
        word.add("c", Rect(10, 2, 15, 12))
        assertEquals("abc", word.toString())
        assertEquals(15, word.width())
        assertEquals(12, word.height())
    }
}
