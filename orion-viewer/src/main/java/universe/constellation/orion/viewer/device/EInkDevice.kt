package universe.constellation.orion.viewer.device

import android.view.View

import universe.constellation.orion.viewer.OrionViewerActivity

abstract class EInkDevice : EInkDeviceWithoutFastRefresh() {

    private var counter: Int = 0

    open val isLightingSupported: Boolean
        get() = false

    override fun flushBitmap() {
        val options = (activity as OrionViewerActivity).globalOptions
        val view = view!!.toView()
        if (options.isEinkOptimization) {
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
        super.flushBitmap()
    }

    open fun doFullUpdate(view: View) {
        super.flushBitmap()
    }

    open fun doDefaultUpdate(view: View) {
        super.flushBitmap()
    }

    @Throws(Exception::class)
    open fun doLighting(delta: Int): Int {
        return -1
    }
}
