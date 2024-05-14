package universe.constellation.orion.viewer.device

import android.view.View

import universe.constellation.orion.viewer.OrionViewerActivity

abstract class EInkDevice : AndroidDevice() {

    private var counter: Int = 0

    private val einkOptimization: Boolean
        get() = options.isEinkOptimization

    override fun flushBitmap(view: View) {
        if (einkOptimization) {
            if (counter < options.einkRefreshAfter) {
                doPartialUpdate(view)
                counter++
            } else {
                counter = 0
                doFullUpdate(view)

            }
        } else {
            doDefaultUpdate(view)
        }
    }

    open fun doPartialUpdate(view: View) {
        super.flushBitmap(view)
    }

    open fun doFullUpdate(view: View) {
        super.flushBitmap(view)
    }

    open fun doDefaultUpdate(view: View) {
        super.flushBitmap(view)
    }

    @Throws(Exception::class)
    open fun doLighting(delta: Int): Int {
        return -1
    }
}
