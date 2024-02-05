package universe.constellation.orion.viewer.selection

import android.graphics.Point
import androidx.core.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.view.OrionDrawScene

enum class State {
    UNDEFINED,
    MOVE,
    DOUBLE_TAP,
    SCALE;
}

open class NewTouchProcessor(val view: OrionDrawScene, val activity: OrionViewerActivity) : GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetectorCompat(activity, this)

    protected var state = State.UNDEFINED

    protected var nextState = State.UNDEFINED

    private val enableTouchMove = activity.globalOptions.isEnableTouchMove

    private val start0 = Point()
    private val last0 = Point()

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
        log("onDown")
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