package universe.constellation.orion.viewer

/**
 * Maps document pages (0-based) to the page numbers printed in the book.
 *
 * [offset] is added to the plain 1-based number, so front matter ends up below 1 and is labelled
 * with roman numerals of its plain number. With [pagesPerSheet] == 2 every document page is a
 * scanned spread holding two book pages.
 */
data class PageNumbering(
    val offset: Int = 0,
    val pagesPerSheet: Int = 1
) {
    private val perSheet: Int
        get() = if (pagesPerSheet == 2) 2 else 1

    val isSpread: Boolean
        get() = perSheet == 2

    fun firstOn(docPage: Int): Int = docPage * perSheet + 1 + offset

    fun lastOn(docPage: Int): Int = firstOn(docPage) + perSheet - 1

    /** Book pages on [docPage]: one, or the two of a spread. */
    fun numbersOn(docPage: Int): IntRange = firstOn(docPage)..lastOn(docPage)

    /** The document page holding the book page [number]; numbers outside the document give its first or last page. */
    fun docPageOf(number: Int, pageCount: Int): Int =
        (number - 1 - offset).floorDiv(perSheet).coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    /** The offset that makes [printedNumber] the first book page of [docPage]. */
    fun offsetFor(docPage: Int, printedNumber: Int): Int = printedNumber - (docPage * perSheet + 1)

    fun label(number: Int): String = if (number >= 1) number.toString() else toRoman(number - offset)

    fun label(numbers: IntRange): String =
        if (numbers.first == numbers.last) label(numbers.first) else "${label(numbers.first)}-${label(numbers.last)}"

    fun sheetLabel(docPage: Int): String = label(numbersOn(docPage))

    /** Inverse of [label] for a single number. */
    fun parse(text: String): Int? {
        val trimmed = text.trim()
        return trimmed.toIntOrNull() ?: fromRoman(trimmed)?.let { it + offset }?.takeIf { it < 1 }
    }

    companion object {
        private val ROMAN = listOf(
            1000 to "m", 900 to "cm", 500 to "d", 400 to "cd", 100 to "c", 90 to "xc",
            50 to "l", 40 to "xl", 10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i"
        )

        internal fun toRoman(number: Int): String {
            if (number !in 1..3999) return "[$number]"
            var rest = number
            return buildString {
                for ((value, digits) in ROMAN) {
                    while (rest >= value) {
                        append(digits)
                        rest -= value
                    }
                }
            }
        }

        internal fun fromRoman(text: String): Int? {
            var rest = text.lowercase()
            if (rest.isEmpty()) return null
            var result = 0
            for ((value, digits) in ROMAN) {
                while (rest.startsWith(digits)) {
                    result += value
                    rest = rest.substring(digits.length)
                }
            }
            return result.takeIf { rest.isEmpty() && toRoman(it) == text.lowercase() }
        }
    }
}
