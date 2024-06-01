package universe.constellation.orion.viewer.selection

import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.dpToPixels
import kotlin.math.abs

enum class State {
    UNDEFINED,
    MOVE,
    DOUBLE_TAP,
    SCALE;
}

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
        detector.setIsLongpressEnabled(true)
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

        log("onDown")
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
        println("onScroll $enableTouchMove")
        if (!enableTouchMove) {
            return false
        }
        nextState = State.MOVE
        view.pageLayoutManager?.doScroll(e2.x, e2.y, -distanceX, -distanceY) ?: return false
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (isFlingActiveOnDown) {
            return true
        }

        log("onSingleTapConfirmed")
        resetNextState()
        doAction(e.x.toInt(), e.y.toInt(), false)
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        log("onDoubleTap")
        nextState = State.DOUBLE_TAP
        activity.doubleClickAction(e.x.toInt(), e.y.toInt())
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (isFlingActiveOnDown) {
            return
        }

        log("onLongPress $state $nextState")
        if (state != State.UNDEFINED) return
        doAction(e.x.toInt(), e.y.toInt(), true)
    }

    private fun doAction(x: Int, y: Int, isLongClick: Boolean) {
        val width = view.sceneWidth
        val height = view.sceneHeight

        val i = 3 * y / height
        val j = 3 * x / width

        val code = activity.globalOptions.getActionCode(i, j, isLongClick)
        activity.doAction(code)
    }

}