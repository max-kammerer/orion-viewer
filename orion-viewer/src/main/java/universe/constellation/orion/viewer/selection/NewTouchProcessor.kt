package universe.constellation.orion.viewer.selection

import android.graphics.Point
import android.support.v4.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.device.EInkDevice

enum class State {
    UNDEFINED,
    MOVE,
    DOUBLE_TAP,
    SCALE;
}

open class NewTouchProcessor(val view: OrionScene, val activity: OrionViewerActivity) : GestureDetector.SimpleOnGestureListener() {

    val detector = GestureDetectorCompat(activity, this)

    protected var state = State.UNDEFINED

    protected var nextState = State.UNDEFINED

    private var info: LayoutPosition? = null

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
                view.afterScaling()
                activity.controller.translateAndZoom(false, 1f, (-last0.x + start0.x).toFloat(), (-last0.y + start0.y).toFloat())
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

    open protected fun onChangingState() {
        if (nextState == State.UNDEFINED) {
            log("onChangingState")
            reset()
        }
    }

    open protected fun reset() {
        state = State.UNDEFINED
        info = null
        start0.x = -1
        start0.y = -1
        last0.x = -1
        last0.y = -1
    }

    open protected fun resetNextState() {
        log("resetNextState")
        nextState = State.UNDEFINED
    }

    override fun onDown(e: MotionEvent?): Boolean {
        log("onDown")
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

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!enableTouchMove) {
            return false
        }

        if (state == State.UNDEFINED) {
            info = view.info?.deepCopy() ?: return true
            start0.x = e1.x.toInt()
            start0.y = e1.y.toInt()
            nextState = State.MOVE
        } else {
            //check same event
        }

        last0.x = e2.x.toInt()
        last0.y = e2.y.toInt()
        val width = view.sceneWidth

        if (insideViewWidth(view.info)) {
            last0.x = start0.x
        } else {
            val delta = last0.x - start0.x
            val offset = -info!!.x.offset
            if (delta < 0) {
                if (offset + info!!.x.pageDimension + delta < width) {
                    last0.x = start0.x - offset - info!!.x.pageDimension + width
                }
            } else {
                if (offset + delta > 0) {
                    last0.x = start0.x - offset
                }
            }
        }

        view.beforeScaling()
        view.doScale(1f, start0, last0, true)
        view.postInvalidate()
        return true
    }

    private fun doAction(x: Int, y: Int, isLongClick: Boolean) {
        val width = view.sceneWidth
        val height = view.sceneHeight

        val i = 3 * y / height
        val j = 3 * x / width

        val code = activity.globalOptions.getActionCode(i, j, isLongClick)
        activity.doAction(code)
    }

    private fun isRightHandSide(x: Int): Boolean {
        return view.sceneWidth - x < 75
    }

    private fun isSupportLighting(): Boolean {
        val device = activity.device
        return device is EInkDevice && device.isLightingSupported
    }



    private val toast by lazy {
        Toast.makeText(activity, "-1", Toast.LENGTH_SHORT)
    }

    private fun doLighting(delta: Int) {
        val device = activity.device
        if (device is EInkDevice) {
            try {
                val newBrightness = device.doLighting(delta / 5)
                if (false) {
                    toast!!.setText("" + newBrightness)
                    toast!!.show()
                }
            } catch (e: Exception) {
                toast!!.setText("Error " + e.message + " " + e.cause)
                toast!!.show()
                log(e)
            }

        }
    }

    private fun insideViewWidth(info: LayoutPosition?): Boolean {
        return info != null && info.x.pageDimension <= view.sceneWidth
    }
}