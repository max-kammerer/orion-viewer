package universe.constellation.orion.viewer.selection

import android.graphics.Point
import android.support.v4.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.android.touch.ScaleGestureDetectorOld
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.util.DensityUtil
import universe.constellation.orion.viewer.view.OrionDrawScene

/**
 * Created by mike on 14.07.16.
 */
enum class State {
    UNDEFINED,
    MOVE,
    SCALE;
}

open class NewTouchProcessor(val view: OrionDrawScene, val activity: OrionViewerActivity) : GestureDetector.SimpleOnGestureListener() {

    val detector = GestureDetectorCompat(activity, this)

    protected var state = State.UNDEFINED

    protected var nextState = State.UNDEFINED

    private var info: LayoutPosition? = null

    private val MOVE_THRESHOLD: Int = with(DensityUtil.calcScreenSize(40, activity)) {
        (this * this).toInt()
    }

    private val enableTouchMove = activity.globalOptions.isEnableTouchMove

    private val start0 = Point()
    private val last0 = Point()

    init {
        detector.setIsLongpressEnabled(true)
        detector.setOnDoubleTapListener(this)
    }

    open fun onTouch(e: MotionEvent): Boolean {
        println("state $state $nextState")
        var onTouchEvent = detector.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_UP && state == State.MOVE) {
            view.afterScaling()
            activity.controller.translateAndZoom(false, 1f, (-last0.x + start0.x).toFloat(), (-last0.y + start0.y).toFloat())
            resetNextState()
            onTouchEvent = true
        }

        if (nextState != state) {
           onChangingState()
        }
        state = nextState
        return onTouchEvent
    }

    open protected fun onChangingState() {
        if (nextState == State.UNDEFINED) {
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
        nextState = State.UNDEFINED
    }

    override fun onDown(e: MotionEvent?): Boolean {
        resetNextState()
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        resetNextState()
        doAction(e.x.toInt(), e.y.toInt(), false)
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        resetNextState()
        activity.doubleClickAction(e.x.toInt(), e.y.toInt())
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        doAction(e.x.toInt(), e.y.toInt(), true)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!enableTouchMove) {
            return false
        }
        println("onScroll $state")

        if (state == State.UNDEFINED) {
            info = view.info?.clone()
            start0.x = e1.x.toInt()
            start0.y = e1.y.toInt()
            nextState = State.MOVE
        } else {
            //check same event
        }

        last0.x = e2.x.toInt()
        last0.y = e2.y.toInt()
        val width = view.width

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
        println("action " + e2.action)
        return true
    }

    private fun doAction(x: Int, y: Int, isLongClick: Boolean) {
        val width = view.width
        val height = view.height

        val i = 3 * y / height
        val j = 3 * x / width

        val code = activity.globalOptions.getActionCode(i, j, isLongClick)
        activity.doAction(code)
    }

    private fun isRightHandSide(x: Int): Boolean {
        return view.width - x < 75
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
                Common.d(e)
            }

        }
    }



    private fun insideViewWidth(info: LayoutPosition?): Boolean {
        return info != null && info.x.pageDimension <= view.width
    }
}