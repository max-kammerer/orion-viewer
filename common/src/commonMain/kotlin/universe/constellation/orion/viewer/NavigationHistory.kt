package universe.constellation.orion.viewer

/**
 * Back/forward stacks over the [DocPlace]s left by jumps (outline, goto, bookmark, search).
 */
class NavigationHistory(private val limit: Int = 100) {

    private val backStack = ArrayDeque<DocPlace>()
    private val forwardStack = ArrayDeque<DocPlace>()

    val canGoBack: Boolean
        get() = backStack.isNotEmpty()

    val canGoForward: Boolean
        get() = forwardStack.isNotEmpty()

    /** Remembers the place being left by a jump. A new jump invalidates the forward branch. */
    fun leave(place: DocPlace) {
        forwardStack.clear()
        if (backStack.lastOrNull()?.isSamePlace(place) == true) return
        backStack.addLast(place)
        while (backStack.size > limit) backStack.removeFirst()
    }

    /** Returns the place to go back to, remembering [current] for [forward]. */
    fun back(current: DocPlace): DocPlace? {
        val target = backStack.removeLastOrNull() ?: return null
        forwardStack.addLast(current)
        return target
    }

    /** Returns the place to go forward to, remembering [current] for [back]. */
    fun forward(current: DocPlace): DocPlace? {
        val target = forwardStack.removeLastOrNull() ?: return null
        backStack.addLast(current)
        return target
    }

    /** Places a back navigation would return to, oldest first. */
    fun backEntries(): List<DocPlace> = backStack.toList()

    /** Places a forward navigation would go to, nearest first. */
    fun forwardEntries(): List<DocPlace> = forwardStack.asReversed().toList()

    /**
     * Multi-step [back] to the [index]-th entry of [backEntries]: the entries after it and
     * [current] become forward entries, browser-style.
     */
    fun backTo(index: Int, current: DocPlace): DocPlace? {
        if (index !in backStack.indices) return null
        var cur = current
        var target: DocPlace? = null
        while (backStack.size > index) {
            target = back(cur) ?: break
            cur = target
        }
        return target
    }

    /** Multi-step [forward] to the [index]-th entry of [forwardEntries]. */
    fun forwardTo(index: Int, current: DocPlace): DocPlace? {
        var cur = current
        var target: DocPlace? = null
        repeat(index + 1) {
            val next = forward(cur) ?: return target
            target = next
            cur = next
        }
        return target
    }

    fun clear() {
        backStack.clear()
        forwardStack.clear()
    }

    /** "page:x:y,page:x:y|page:x:y": back stack, oldest first, then the forward stack. */
    fun serialize(): String {
        return backStack.joinToString(",") { it.serialize() } + "|" + forwardStack.joinToString(",") { it.serialize() }
    }

    fun restore(serialized: String) {
        clear()
        if (serialized.isBlank()) return
        val (back, forward) = serialized.split("|", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        back.split(",").mapNotNullTo(backStack) { DocPlace.parse(it) }
        forward.split(",").mapNotNullTo(forwardStack) { DocPlace.parse(it) }
    }
}
