package universe.constellation.orion.viewer.scene

import android.os.Handler
import android.os.Looper
import org.jetbrains.anko.AnkoAsyncContext
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore

/**
 * Created by mike on 1/4/17.
 */

val orionExecutor: ExecutorService = Executors.newSingleThreadExecutor()

fun <T> T.orionAsync(
        exceptionHandler: ((Throwable) -> Unit)? = null,
        task: AnkoAsyncContext<T>.() -> Unit
): Future<Unit> {
    val context = AnkoAsyncContext(WeakReference(this))
    return orionExecutor.submit<Unit> {
        try {
            context.task()
        } catch (thr: Throwable) {
            exceptionHandler?.invoke(thr)
        }
    }
}

fun <T, R> AnkoAsyncContext<T>.uiThreadAndWait(f: (T) -> R): R? {
    val ref = weakRef.get() ?: return null
    if (ContextHelper.mainThread == Thread.currentThread()) {
        return f(ref)
    } else {
        val semaphore = Semaphore(0)
        var result: R? = null
        if (ContextHelper.handler.post {
            result = f(ref)
            semaphore.release()
            d("semaphore release")
        })  {
            d("semaphore wait")
            semaphore.acquire()
            d("semaphore acquire")
        }
        else {
            return null
        }
        return result
    }
}



fun checkUIThread() {
    if (Looper.myLooper() !== Looper.getMainLooper()) {
        throw RuntimeException("Wrong thread")
    }
}

fun checkNonUIThread() {
    if (Looper.myLooper() === Looper.getMainLooper()) {
        throw RuntimeException("Wrong thread")
    }
}


private object ContextHelper {
    val handler = Handler(Looper.getMainLooper())
    val mainThread = Looper.getMainLooper().thread
}