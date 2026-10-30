package universe.constellation.orion.viewer.formats

import android.content.res.Resources
import universe.constellation.orion.viewer.EngineLibraryMissingException
import universe.constellation.orion.viewer.FileContent
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.fileTailContains
import java.io.File

/**
 * Why a book can't be shown: what the user is told, [describe], and what goes to the reports,
 * [report]. Problems of the file itself (empty, another format, no pages) are reported as
 * warnings of their own instead of joining the engines' errors.
 */
sealed class OpenProblem {

    abstract fun describe(resources: Resources): String

    abstract val report: Throwable

    /** The error whose message the dialog adds to [describe], if any. */
    open val error: Throwable? get() = null

    class Empty(private val description: String) : OpenProblem() {
        override fun describe(resources: Resources) = resources.cantOpen(R.string.fileopen_file_is_emppty)
        override val report get() = RuntimeException("Warning: empty file, $description")
    }

    /** The apk, not the file: tells what to do instead of the file name. */
    class NoEngine(override val error: EngineLibraryMissingException) : OpenProblem() {
        override fun describe(resources: Resources) =
            resources.getString(R.string.crash_on_book_opening_no_engine_library, error.abis)
        override val report get() = error
    }

    /** Something else under the book's name: a web page, an archive, encrypted or zeroed data. */
    class Mismatch(val mismatch: ContentMismatch, override val error: Throwable) : OpenProblem() {
        override fun describe(resources: Resources) =
            resources.getString(R.string.fileopen_cant_open, mismatch.describe(resources))
        override val report get() = RuntimeException("Warning: $mismatch", error)
    }

    /** The engine failed on what looks like its own format: a damaged book. */
    class EngineError(private val fileName: String, override val error: Throwable) : OpenProblem() {
        override fun describe(resources: Resources) =
            resources.getString(R.string.crash_on_book_opening_message_header_panel, fileName)
        override val report get() = error
    }

    /**
     * Opened without pages. mupdf opens a .zip or .tar without pictures inside as a comic book
     * of no pages: usually a book packed for sending, to be extracted first. [content] is what
     * detectFileContent tells, [structure] the document's describeStructure, [host] the intent's:
     * where such files come from.
     */
    class NoPages(private val content: FileContent?, file: File, structure: String?, host: String?) : OpenProblem() {
        private val details = listOfNotNull("host=$host", "size=${file.length()}", structure).joinToString(", ")

        /* The start of a PDF without its end: the page tree is usually in the lost part. */
        private val isTruncatedPdf = content == FileContent.PDF && fileTailContains(file.path, "%%EOF") == false

        override fun describe(resources: Resources) = if (content?.isArchive == true) {
            resources.getString(R.string.fileopen_cant_open, resources.getString(R.string.fileopen_content_archive, content.shortName))
        } else if (isTruncatedPdf) {
            resources.cantOpen(R.string.fileopen_incomplete_file)
        } else {
            resources.cantOpen(R.string.fileopen_no_pages)
        }
        override val report get() = RuntimeException(
            when {
                content?.isArchive == true -> "Warning: archive without pages, $details"
                isTruncatedPdf -> "Warning: incomplete file, $details"
                else -> "Warning: no pages in doc, $details"
            }
        )
    }

    /** Opened, but failed while being shown. */
    class Failed(override val error: Throwable) : OpenProblem() {
        override fun describe(resources: Resources) = resources.getString(R.string.crash_on_book_opening_title)
        override val report get() = error
    }

    companion object {

        /** What made the engine fail on [file]; reads the start of the file. */
        fun ofOpenError(file: File, error: Exception): OpenProblem {
            if (error is EngineLibraryMissingException) return NoEngine(error)
            return findContentMismatch(file)?.let { Mismatch(it, error) } ?: EngineError(file.name, error)
        }
    }
}

private fun Resources.cantOpen(reason: Int) = getString(R.string.fileopen_cant_open, getString(reason))
