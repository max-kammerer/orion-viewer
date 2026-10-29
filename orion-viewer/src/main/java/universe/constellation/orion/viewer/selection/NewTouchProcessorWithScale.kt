package universe.constellation.orion.viewer.selection

import universe.constellation.orion.viewer.prefs.GlobalOptions
import androidx.core.view.ScaleGestureDetectorCompat
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.util.MoveUtil
import universe.constellation.orion.viewer.view.OrionDrawScene

class NewTouchProcessorWithScale(view: OrionDrawScene, activity: OrionViewerActivity) :
        NewTouchProcessor(view, activity),
        ScaleGestureDetector.OnScaleGestureListener {

    private val scaleDetector = ScaleGestureDetector(activity, this)

    init {
        /* Quick scale, a double tap held and dragged, zooms with one finger. Only while the double
           tap has no action of its own: after a double tap that selects a word the finger still
           on the screen would zoom at the slightest move (issue #77). */
        activity.globalOptions.DOUBLE_TAP_ACTION.observe(activity) {
            ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleDetector, it == GlobalOptions.DISABLE)
        }
    }

    //read per gesture: the option applies without a restart
    private val enableTouchMoveOnPinchZoom: Boolean
        get() = activity.globalOptions.ENABLE_MOVE_ON_PINCH_ZOOM.value

    private val startFocus = PointF()
    private val endFocus = PointF()
    private var curScale = 1.0F

    private var startController: Controller? = null

    /*
     * From a pinch till the last finger is up. The fingers are rarely lifted together: the pinch
     * ends with the first one, and the one left would scroll the page from where the pinch put it,
     * dropping the frozen frame of the pinch while the pages are still being laid out (a blank
     * page with the loading sign flashed). The rest of the touch is ignored.
     */
    private var afterPinch = false

    override fun onTouch(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        if (scaleDetector.isInProgress) {
            return true
        }
        if (afterPinch) {
            val action = e.actionMasked
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                afterPinch = false
                //the gesture detectors saw this touch begin: they see it end as cancelled, no tap or fling
                val cancel = MotionEvent.obtain(e)
                cancel.action = MotionEvent.ACTION_CANCEL
                super.onTouch(cancel)
                cancel.recycle()
            }
            return true
        }
        return super.onTouch(e)
    }

    override fun onChangingState() {
        if (nextState == State.SCALE) {
            curScale = scaleDetector.scaleFactor
            //redundant?
            startFocus.set(scaleDetector.focusX, scaleDetector.focusY)
            endFocus.set(startFocus)
        }
        super.onChangingState()
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        log("OnScaleBegin")
        curScale = detector.scaleFactor
        startFocus.set(detector.focusX, detector.focusY)
        nextState = State.SCALE
        startController = activity.controller
        afterPinch = true
        return true
    }

    override fun reset() {
        super.reset()
        curScale = 1F
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        log("OnScaleEnd")
        resetNextState()
        val newX = MoveUtil.calcOffset(startFocus.x, endFocus.x, curScale, enableTouchMoveOnPinchZoom)
        val newY = MoveUtil.calcOffset(startFocus.y, endFocus.y, curScale, enableTouchMoveOnPinchZoom)
        if (activity.controller === startController) {
            view.freezeFrame()
            activity.controller?.translateAndZoom(curScale, endFocus, newX, newY)
        }
        view.disableScalingMode()
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        log("onScale")
        curScale *= detector.scaleFactor
        endFocus.set(detector.focusX, detector.focusY)
        view.enableScalingMode()
        view.doScale(curScale, startFocus, endFocus, enableTouchMoveOnPinchZoom)
        view.invalidate()
        return true
    }
}