package universe.constellation.orion.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageNumberingTest {

    @Test
    fun plainNumbering() {
        val numbering = PageNumbering()
        assertEquals(1..1, numbering.numbersOn(0))
        assertEquals("28", numbering.sheetLabel(27))
        assertEquals(27, numbering.docPageOf(28, 100))
        assertEquals(99, numbering.docPageOf(1000, 100))
        assertEquals(0, numbering.docPageOf(-5, 100))
    }

    @Test
    fun offsetAndFrontMatter() {
        val numbering = PageNumbering(offset = PageNumbering().offsetFor(docPage = 12, printedNumber = 1))
        assertEquals(-12, numbering.offset)
        assertEquals("1", numbering.sheetLabel(12))
        assertEquals("xii", numbering.sheetLabel(11))
        assertEquals("i", numbering.sheetLabel(0))
        assertEquals(12, numbering.docPageOf(1, 100))
        assertEquals(0, numbering.parse("xii"))
        assertEquals(11, numbering.docPageOf(numbering.parse("xii")!!, 100))
        assertEquals(15, numbering.parse(" 15 "))
        assertNull(numbering.parse("abc"))
        assertNull(numbering.parse("iiii"))
        //roman labels exist for front matter only
        assertNull(numbering.parse("xiii"))
    }

    @Test
    fun positiveOffset() {
        val numbering = PageNumbering(offset = 100)
        assertEquals("101", numbering.sheetLabel(0))
        assertEquals(0, numbering.docPageOf(50, 10))
        assertEquals(4, numbering.docPageOf(105, 10))
    }

    @Test
    fun spread() {
        val base = PageNumbering(pagesPerSheet = 2)
        //document page 5 shows printed pages 8 and 9
        val numbering = base.copy(offset = base.offsetFor(5, 8))
        assertEquals(-3, numbering.offset)
        assertEquals("8-9", numbering.sheetLabel(5))
        assertEquals("iii-1", numbering.sheetLabel(1))
        assertEquals(5, numbering.docPageOf(8, 100))
        assertEquals(5, numbering.docPageOf(9, 100))
        assertEquals(99, numbering.docPageOf(5000, 100))

        assertEquals(8..9, numbering.numbersOn(5))
    }
}
