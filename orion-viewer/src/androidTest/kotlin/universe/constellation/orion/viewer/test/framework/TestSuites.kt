package universe.constellation.orion.viewer.test.framework

import org.junit.runner.Runner
import org.junit.runners.Suite
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.TestWithParameters

enum class BookSet {
    /** The well-known default books: complex scenarios relying on size/structure. */
    MAIN,

    /** Every file in the device test folder: broad smoke sweeps. */
    ALL;

    fun books(): List<BookFile> = when (this) {
        MAIN -> BookFile.mainTestEntries()
        ALL -> BookFile.testEntriesWithCustoms()
    }
}

/** Picks the book set a [BookSetRunner]-run suite is parameterized with; default is [BookSet.ALL]. */
@java.lang.annotation.Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Books(val value: BookSet)

/**
 * Like [org.junit.runners.Parameterized] with a single book parameter, but the set
 * of books is declared with the [Books] annotation instead of a @Parameters override.
 */
class BookSetRunner(klass: Class<*>) : Suite(klass, buildRunners(klass)) {

    companion object {
        private fun buildRunners(klass: Class<*>): List<Runner> {
            val set = klass.getAnnotation(Books::class.java)?.value ?: BookSet.ALL
            return set.books().map { book ->
                BlockJUnit4ClassRunnerWithParameters(
                    TestWithParameters("[Test for $book book]", TestClass(klass), listOf(book))
                )
            }
        }
    }
}
