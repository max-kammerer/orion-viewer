package universe.constellation.orion.viewer.view

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.errorInDebug

open class CorePageView(val pageNum: Int,
                   val document: Document,
                   val controller: Controller,
                   rootJob: Job,
                    val page: Page = document.getOrCreatePageAdapter(pageNum)) {
    private val analytics = controller.activity.analytics

    private val handler = CoroutineExceptionHandler { _, ex ->
        errorInDebug("Processing error for page $pageNum", ex)
        analytics.error(ex)
    }

    private val renderingPageJobs = SupervisorJob(rootJob)

    private val dataPageJobs = SupervisorJob(rootJob)

    @Volatile
    private var rawSize: Deferred<PageSize>? = null

    @Volatile
    private var pageData: Deferred<Unit>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    protected  val renderingScope = CoroutineScope(
        controller.renderingDispatcher.limitedParallelism(2) + renderingPageJobs + handler
    )

    protected val dataPageScope = CoroutineScope(controller.context + dataPageJobs + handler)


    fun readPageDataFromUI(): Deferred<Unit> {
        if (pageData == null) {
            pageData = dataPageScope.async { page.readPageDataForRendering() }
        }
        return pageData!!
    }

    fun readRawSizeFromUI(): Deferred<PageSize> {
        if (rawSize == null) {
            rawSize = dataPageScope.async { page.getPageSize() }
        }
        return rawSize!!
    }


    internal fun cancelChildJobs(allJobs: Boolean = false) {
        renderingPageJobs.cancelChildren()
        if (allJobs) {
            dataPageJobs.cancelChildren()
        }
    }

    protected suspend fun waitJobsCancellation(allJobs: Boolean = false) {
        if (allJobs) {
            dataPageJobs.cancel()
        }
        renderingPageJobs.cancelAndJoin()
        if (allJobs) {
            dataPageJobs.cancelAndJoin()
        }
    }

    suspend fun <T> renderForCrop(body: suspend () -> T): T {
        return withContext(controller.renderingDispatcher) {
            body()
        }
    }

}