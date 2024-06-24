package universe.constellation.orion.viewer.selection

import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import universe.constellation.orion.viewer.ContextAction
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.dpToPixels
import universe.constellation.orion.viewer.prefs.GlobalOptions
import kotlin.math.abs

enum class State {
    UNDEFINED,
    MOVE,
    DOUBLE_TAP,
    SCALE;
}

enum class ClickType {
    SHORT,
    LONG,
    DOUBLE
}

class ClickInfo(val x: Int, val y: Int, val clickType: ClickType)

open class NewTouchProcessor(val view: OrionDrawScene, val activity: OrionViewerActivity) : GestureDetector.SimpleOnGestureListener() {

    private val minFlingDistance = activity.dpToPixels(32f)

    private val property = object : FloatPropertyCompat<View>("doScroll") {
        var prevValue = 0f

        override fun getValue(view2: View): Float {
            return prevValue
        }

        override fun setValue(view2: View, value: Float) {
            val delta = prevValue - value
            view.pageLayoutManager?.doScroll(0f, 0f, 0f, -delta)
            prevValue = value
        }
    }

    private val flingAnim =
        FlingAnimation(
            view,
            property
        )
            .setMinValue(-20000f)
            .setFriction(0.5f)
            .setMaxValue(20000f)

    private val detector = GestureDetectorCompat(activity, this)

    protected var state = State.UNDEFINED

    protected var nextState = State.UNDEFINED

    private val enableTouchMove = activity.globalOptions.isEnableTouchMove

    private val start0 = Point()
    private val last0 = Point()

    private var isFlingActiveOnDown = false

    init {
        activity.globalOptions.LONG_TAP_ACTION.observe(activity) {
            detector.setIsLongpressEnabled(it != GlobalOptions.DISABLE)
        }

        detector.setOnDoubleTapListener(this)
    }

    open fun onTouch(e: MotionEvent): Boolean {
        log("onTouch state = $state")
        var onTouchEvent = detector.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_UP) {
            if (state == State.MOVE) {
                resetNextState()
                onTouchEvent = true
            }
            if (state == State.DOUBLE_TAP)  {
                resetNextState()
                onTouchEvent = true
            }
        }
        log("onTouch nextState = $nextState")
        if (nextState != state) {
           onChangingState()
        }
        state = nextState
        return onTouchEvent
    }

    protected open fun onChangingState() {
        if (nextState == State.UNDEFINED) {
            log("onChangingState")
            reset()
        }
    }

    protected open fun reset() {
        state = State.UNDEFINED
        start0.x = -1
        start0.y = -1
        last0.x = -1
        last0.y = -1
    }

    protected open fun resetNextState() {
        log("resetNextState")
        nextState = State.UNDEFINED
    }

    override fun onDown(e: MotionEvent): Boolean {
        isFlingActiveOnDown = flingAnim.isRunning
        if (isFlingActiveOnDown) {
            flingAnim.cancel()
        }

        log("gesture: onDown")
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (abs(velocityX) > abs(velocityY)) return false
        if (e1 == null || abs(velocityY) < 500) return false
        val yDist = abs(e1.y - e2.y)
        if (abs(e1.x - e2.x) > yDist) return false
        if (yDist < minFlingDistance) return false

        flingAnim.cancel()
        property.prevValue = 0f
        flingAnim.setStartVelocity(velocityY)
        flingAnim.start()
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        log("gesture: onScroll $enableTouchMove")
        if (!enableTouchMove) {
            return false
        }
        nextState = State.MOVE
        view.pageLayoutManager?.doScroll(e2.x, e2.y, -distanceX, -distanceY) ?: return false
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (activity.globalOptions.DOUBLE_TAP_ACTION.value == GlobalOptions.DISABLE) {
            return processSingleTap(e, "onSingleTapUp")
        }
        return false
    }


    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (activity.globalOptions.DOUBLE_TAP_ACTION.value != GlobalOptions.DISABLE) {
            return processSingleTap(e, "onSingleTapConfirmed")
        }
        return false
    }

    private fun processSingleTap(e: MotionEvent, msg: String): Boolean {
        if (isFlingActiveOnDown) {
            return true
        }

        log("processSingleTap: $msg")
        resetNextState()
        doAction(ContextAction.TAP_ACTION, e, ClickType.SHORT)
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (activity.globalOptions.DOUBLE_TAP_ACTION.value == GlobalOptions.DISABLE) {
            return processSingleTap(e, "onDoubleTap skipped")
        }

        log("gesture: onDoubleTap")
        nextState = State.DOUBLE_TAP
        doAction(
            ContextAction.findAction(activity.globalOptions.DOUBLE_TAP_ACTION.value),
            e,
            ClickType.DOUBLE
        )
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (isFlingActiveOnDown) {
            return
        }

        log("gesture: onLongPress $state $nextState")
        if (state != State.UNDEFINED) return
        doAction(
            ContextAction.findAction(activity.globalOptions.LONG_TAP_ACTION.value),
            e,
            ClickType.LONG
        )
    }

    private fun doAction(action: ContextAction?, e: MotionEvent, clickType: ClickType) {
        action?.doAction(activity, ClickInfo(e.x.toInt(), e.y.toInt(), clickType))
    }

}