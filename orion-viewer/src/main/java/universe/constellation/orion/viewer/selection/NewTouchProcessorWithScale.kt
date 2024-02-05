package universe.constellation.orion.viewer.selection

import android.graphics.PointF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.util.MoveUtil
import universe.constellation.orion.viewer.view.OrionDrawScene

class NewTouchProcessorWithScale(view: OrionDrawScene, activity: OrionViewerActivity) :
        NewTouchProcessor(view, activity),
        ScaleGestureDetector.OnScaleGestureListener {

    private val scaleDetector = ScaleGestureDetector(activity, this)

    private val enableTouchMoveOnPinchZoom = activity.globalOptions.isEnableMoveOnPinchZoom

    private val startFocus = PointF()
    private val endFocus = PointF()
    private var curScale = 1.0F

    override fun onTouch(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        if (scaleDetector.isInProgress) {
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
        println("OnScaleBegin")
        curScale = detector.scaleFactor
        startFocus.set(detector.focusX, detector.focusY)
        nextState = State.SCALE
        return true
    }

    override fun reset() {
        super.reset()
        curScale = 1F
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        println("OnScaleEnd")
        resetNextState()
        val newX = MoveUtil.calcOffset(startFocus.x, endFocus.x, curScale, enableTouchMoveOnPinchZoom)
        val newY = MoveUtil.calcOffset(startFocus.y, endFocus.y, curScale, enableTouchMoveOnPinchZoom)
        activity.controller!!.translateAndZoom(curScale, startFocus, endFocus, newX, newY)
        view.inNormalMode()
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        println("onScale")
        curScale *= detector.scaleFactor
        endFocus.set(detector.focusX, detector.focusY)
        view.inScalingMode()
        view.doScale(curScale, startFocus, endFocus, enableTouchMoveOnPinchZoom)
        view.invalidate()
        return true
    }
}