package universe.constellation.orion.viewer.selection

import android.graphics.Point
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import universe.constellation.orion.viewer.OrionScene
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.util.MoveUtil

@RequiresApi(api = Build.VERSION_CODES.FROYO)
class NewTouchProcessorWithScale(view: OrionScene, activity: OrionViewerActivity) :
        NewTouchProcessor(view, activity),
        ScaleGestureDetector.OnScaleGestureListener {

    val scaleDetector = ScaleGestureDetector(activity, this)

    private val enableTouchMoveOnPinchZoom = activity.globalOptions.isEnableMoveOnPinchZoom

    private val startFocus = Point()
    private val endFocus = Point()
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
            startFocus.x = scaleDetector.focusX.toInt()
            startFocus.y = scaleDetector.focusY.toInt()
            endFocus.x = startFocus.x
            endFocus.y = startFocus.y
        }
        super.onChangingState()
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        curScale = detector.scaleFactor
        startFocus.x = detector.focusX.toInt()
        startFocus.y = detector.focusY.toInt()
        nextState = State.SCALE
        return true
    }

    override fun reset() {
        super.reset()
        curScale = 1F
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        resetNextState()
        val newX = MoveUtil.calcOffset(startFocus.x, endFocus.x, curScale, enableTouchMoveOnPinchZoom)
        val newY = MoveUtil.calcOffset(startFocus.y, endFocus.y, curScale, enableTouchMoveOnPinchZoom)
        view.afterScaling()
        //There is no start scale event!!!!
        if (OrionApplication.TEXET_TB176FL) {
            curScale *= detector.scaleFactor
        }
        activity.controller.translateAndZoom(true, curScale, newX, newY)
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        curScale *= detector.scaleFactor
        endFocus.x = detector.focusX.toInt()
        endFocus.y = detector.focusY.toInt()
        view.beforeScaling()
        view.doScale(curScale, startFocus, endFocus, enableTouchMoveOnPinchZoom)
        view.postInvalidate()
        return true
    }
}