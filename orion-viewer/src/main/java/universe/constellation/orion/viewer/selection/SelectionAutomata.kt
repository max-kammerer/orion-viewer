package universe.constellation.orion.viewer.selection

import android.content.DialogInterface
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.dialog.DialogOverView
import universe.constellation.orion.viewer.document.TextAndSelection
import universe.constellation.orion.viewer.dpToPixels
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.view.PageLayoutManager
import kotlin.math.max
import kotlin.math.min

class SelectionAutomata(val activity: OrionViewerActivity) :
    DialogOverView(activity, R.layout.text_selector, android.R.style.Theme_Translucent_NoTitleBar) {
    private enum class STATE {
        START, ACTIVE_SELECTION, MOVING_HANDLER, CANCELED
    }

    private var state = STATE.CANCELED

    private val selectionView: SelectionViewNew = dialog.findViewById(R.id.text_selector)

    private var isSingleWord = false

    private var translate = false

    private val triangleSide: Float

    private val singleWordDelta: Float

    private var activeHandler: Handler? = null

    private var actions: SelectedTextActions? = null

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    private var isRectSelection = true

    init {
        selectionView.setOnTouchListener { v: View?, event: MotionEvent ->
            this@SelectionAutomata.processTouch(
                event
            )
        }
        triangleSide = activity.dpToPixels(30f).toFloat()
        singleWordDelta = activity.dpToPixels(2f).toFloat()
    }

    private fun debug(msg: String) {
        log("touch: $msg")
    }

    private fun processTouch(event: MotionEvent): Boolean {
        val action = event.action
        debug("action: $action oldState: $state")

        val oldState = state
        var result = true
        when (state) {
            STATE.START -> if (action == MotionEvent.ACTION_DOWN) {
                selectionView.reset()
                setStartHandlers(event.x, event.y)

                activeHandler = selectionView.endHandler
                if (!isSingleWord) {
                    state = STATE.MOVING_HANDLER
                }
            } else {
                state = if (isSingleWord && action == MotionEvent.ACTION_UP) {
                    STATE.ACTIVE_SELECTION
                } else if (isSingleWord && action == MotionEvent.ACTION_MOVE) {
                    STATE.START
                } else {
                    STATE.CANCELED
                }
            }

            STATE.ACTIVE_SELECTION -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    activeHandler =
                        selectionView.findClosestHandler(event.x, event.y, 0.8f * triangleSide)
                    println(activeHandler)
                    if (activeHandler == null) {
                        state = STATE.CANCELED
                        result = false
                    } else {
                        state = STATE.MOVING_HANDLER
                        isSingleWord = false
                        actions?.dismissOnlyDialog()
                    }
                } else {
                    //cancel
                }
            }

            STATE.MOVING_HANDLER -> if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE) {
                activeHandler!!.x += event.x - lastX
                activeHandler!!.y += event.y - lastY
                recordLastEvent(event)

                updateSelectionAndGetText(getSelectedText())

                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.ACTIVE_SELECTION
                }
            } else {
                result = false
            }

            else -> result = false
        }

        if (oldState != state) {
            debug("new state: $action oldState: $state")
            when (state) {
                STATE.CANCELED -> {
                    actions?.dismissOnlyDialog()
                    actions = null
                    dialog.dismiss()
                }

                STATE.ACTIVE_SELECTION -> {
                    val text = getSelectedText()
                    if (oldState == STATE.START && isSingleWord) {
                        debug("oldState == STATE.START && isSingleWord")
                        text?.let {
                            it.rect.firstOrNull()?.let {
                                debug("rect: $it")
                                updateHandlersByTextSelection(it)
                            }
                        }
                    }
                    updateSelectionAndGetText(text)
                    showActionsPopupOrDoTranslation(
                        text?.value ?: "",
                        translate,
                        activity.controller!!
                    )
                }

                STATE.MOVING_HANDLER -> {
                    recordLastEvent(event)
                }

                else -> {
                }
            }
        }
        debug("result = $result")
        return result
    }

    private fun recordLastEvent(event: MotionEvent) {
        lastX = event.x
        lastY = event.y
    }

    private fun setHandlers(start: PointF, end: PointF) {
        selectionView.setHandlers(
            Handler(
                start.x,
                start.y,
                triangleSide,
                true
            ), Handler(
                end.x,
                end.y,
                triangleSide,
                false
            )
        )
    }

    private fun updateSelectionAndGetText(text: TextAndSelection?) {
        selectionView.updateView(text?.rect ?: emptyList())
        selectionView.invalidate()
    }

    private fun getSelectedText(): TextAndSelection? {
        return getTextByHandlers(
            selectionView.startHandler!!,
            selectionView.endHandler!!,
            isRect = isRectSelection,
            isSingleWord = isSingleWord
        )
    }

    fun initSelectionByPosition(pos: ClickInfo, translate: Boolean) {
        resetState(true, translate)
        isRectSelection = false

        setStartHandlers(pos.x.toFloat(), pos.y.toFloat())

        val selectedText = getSelectedText()
        val controller = activity.controller ?: return

        selectedText?.rect?.let {
            it.firstOrNull()?.let { rect ->
                updateHandlersByTextSelection(rect)
            }
        }

        updateSelectionAndGetText(selectedText)
        showActionsPopupOrDoTranslation(selectedText?.value ?: "", translate, controller)
    }

    private fun setStartHandlers(x: Float, y: Float) {
        val selection = getScreenSelectionRectWithDelta(
            RectF(
                x,
                y,
                x,
                y,
            ), isSingleWord
        )
        setHandlers(
            PointF(selection.left, selection.top),
            PointF(selection.right, selection.bottom)
        )
    }

    private fun updateHandlersByTextSelection(rect: RectF) {
        selectionView.startHandler!!.apply {
            x = rect.left
            y = rect.top
        }
        selectionView.endHandler!!.apply {
            x = rect.right
            y = rect.bottom
        }
    }

    private fun showActionsPopupOrDoTranslation(
        text: String,
        translate: Boolean,
        controller: Controller
    ) {
        if (translate && text.isNotBlank()) {
            dialog.dismiss()
            Action.DICTIONARY.doAction(controller, activity, text)
        } else {
            val selectedTextActions = SelectedTextActions(activity, dialog)
            actions = selectedTextActions
            val occupiedArea = getScreenSelectionRect(selectionView.startHandler!!, selectionView.endHandler!!)
            occupiedArea.inset(0f, -triangleSide)
            if (!dialog.isShowing) {
                //TODO: refactor
                dialog.setOnShowListener { dialog2: DialogInterface? ->
                    selectedTextActions.show(text, occupiedArea)
                    dialog.setOnShowListener(null)
                }
                startTextSelection(true, translate = false, quite = true)
                state = STATE.ACTIVE_SELECTION
            } else {
                selectedTextActions.show(text, occupiedArea)
            }
        }
    }

    @JvmOverloads
    fun startTextSelection(isSingleWord: Boolean, translate: Boolean, quite: Boolean = false) {
        selectionView.setColorFilter(activity.fullScene.colorStuff.backgroundPaint.colorFilter)
        if (!quite) {
            selectionView.reset()
        }
        initDialogSize()
        dialog.show()
        if (!quite) {
            val msg =
                activity.resources.getString(if (isSingleWord) R.string.msg_select_word else R.string.msg_select_text)
            activity.showFastMessage(msg)
            resetState(isSingleWord, translate)
            isRectSelection = !isSingleWord
        }
    }

    private fun resetState(isSingleWord: Boolean, translate: Boolean) {
        state = STATE.START
        this.isSingleWord = isSingleWord
        this.translate = translate
        this.isRectSelection = true
        actions?.dismissOnlyDialog()
        actions = null
        activeHandler = null
    }

    fun getPageSelectionRectangles(
        rect: RectF,
        isSingleWord: Boolean,
        pageLayoutManager: PageLayoutManager
    ): List<PageAndSelection> {
        val expandedRect = getScreenSelectionRectWithDelta(rect, isSingleWord)
        return pageLayoutManager.findPageAndPageRect(expandedRect.toRect())
    }

    private fun getScreenSelectionRectWithDelta(
        rect: RectF,
        isSingleWord: Boolean
    ): RectF {
        if (!isSingleWord) return rect
        rect.inset(-singleWordDelta, -singleWordDelta) //TODO: dp to pixel
        return rect
    }

    companion object {
        fun getScreenSelectionRect(startHandler: Handler, endHandler: Handler): RectF {
            return RectF(
                min(startHandler.x, endHandler.x),
                min(startHandler.y, endHandler.y),
                max(startHandler.x, endHandler.x),
                max(startHandler.y, endHandler.y)
            )
        }
    }
}
