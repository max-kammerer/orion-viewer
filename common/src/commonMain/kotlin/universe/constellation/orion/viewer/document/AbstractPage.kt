package universe.constellation.orion.viewer.document

import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.logError
import universe.constellation.orion.viewer.timing
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

    protected abstract fun readPageSize(): PageSize?

    override fun getPageSize(): PageSize {
        if (!::pageSize.isInitialized) {
            timing("Page $pageNum size extraction") {
                pageSize = readPageSize() ?: dimensionForCorruptedPage().also {
                    logError("Page $pageNum is corrupted")
                }
            }
            log("Page $pageNum size: $pageSize")
        }
        return pageSize
    }

    private fun dimensionForCorruptedPage() = PageSize(300, 400)

    override fun toString(): String {
        return "Page $pageNum"
    }

    abstract fun destroyInternal()
}