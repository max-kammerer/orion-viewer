package universe.constellation.orion.viewer.document

/**
 * Receives problems a document survives, e.g. a page whose image can't be decoded and is shown
 * blank. Such errors are caught and never reach the crash handler, so without this they would go
 * unnoticed. Called from engine threads.
 */
fun interface NonFatalErrorReporter {

    fun report(message: String, error: Throwable)

    companion object {
        /** For documents opened outside the viewer, e.g. in tests: the problem is only logged. */
        @JvmField
        val NONE = NonFatalErrorReporter { _, _ -> }
    }
}
