package universe.constellation.orion.viewer.view

import android.graphics.RectF
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.PageSize
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.Page
import universe.constellation.orion.viewer.errorInDebug

open class CorePageView(
    val pageNum: Int,
    val document: Document,
    val controller: Controller,
    rootJob: Job,
    val page: Page = document.getOrCreatePageAdapter(pageNum)
) {
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
    protected val renderingScope = CoroutineScope(
        controller.renderingDispatcher.limitedParallelism(2) + renderingPageJobs + handler
    )

    protected val renderingScopeOnUI = renderingScope + Dispatchers.Main

    protected val dataPageScope = CoroutineScope(controller.context + dataPageJobs + handler)


    /**
     * Marks the app busy for the whole life of the job: geometry calculation and page rendering
     * happen in background and leave the main looper empty, so without it instrumentation tests
     * have nothing to wait for but a sleep. In the production build the resource is a no-op.
     */
    protected fun CoroutineScope.launchTracked(
        context: CoroutineContext = EmptyCoroutineContext,
        body: suspend CoroutineScope.() -> Unit
    ): Job {
        val idlingRes = controller.activity.orionApplication.idlingRes
        idlingRes.busy()
        return launch(context, block = body).apply {
            invokeOnCompletion { idlingRes.free() }
        }
    }

    fun readPageDataFromUI(): Deferred<Unit> {
        if (pageData == null) {
            pageData = dataPageScope.async { page.readPageDataForRendering() }.also { data ->
                /* Every tap looks the links up on the UI thread, so they are read here, in the
                 * background, after the page data and before the page shows up on screen. */
                dataPageScope.launchTracked {
                    data.join()
                    page.getLinks()
                }
            }
        }
        return pageData!!
    }

    /**
     * True once [readPageDataFromUI] has finished, i.e. the page is ready to be drawn. Text
     * selection on the UI thread consults only such pages: for pdf their text comes from the
     * display list without touching the document, and a page that isn't there yet has nothing
     * on screen to select anyway.
     */
    fun isPageDataLoaded(): Boolean = pageData?.isCompleted == true

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