package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.logTrace
import universe.constellation.orion.viewer.traceTiming
import java.util.concurrent.atomic.AtomicInteger

abstract class AbstractPage(override val pageNum: Int) : Page {

    private val counter = AtomicInteger(0)

    @Volatile
    protected var destroyed = false

    @Volatile
    private lateinit var pageSize: PageSize

    fun increaseUsages() {
        counter.incrementAndGet()
    }

    fun decreaseUsages(): Int {
        return counter.decrementAndGet()
    }

    @Volatile
    private var links: List<PageLink>? = null

    protected abstract fun readPageSize(): PageSize?

    protected abstract fun readLinks(): List<PageLink>

    override fun getLinks(): List<PageLink> {
        links?.let { return it }
        if (destroyed) return emptyList()
        return traceTiming({ "Page $pageNum links extraction" }) { readLinks() }.also { links = it }
    }

    override fun loadedLinks(): List<PageLink>? = links

    override fun getPageSize(): PageSize {
        if (!::pageSize.isInitialized) {
            traceTiming({ "Page $pageNum size extraction" }) {
                pageSize = readPageSize() ?: dimensionForCorruptedPage().also {
                    logError("Page $pageNum is corrupted")
                }
            }
            logTrace { "Page $pageNum size: $pageSize" }
        }
        return pageSize
    }

    private fun dimensionForCorruptedPage() = PageSize(300, 400)

    override fun toString(): String {
        return "Page $pageNum"
    }

    abstract fun destroyInternal()
}