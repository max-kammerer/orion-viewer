package universe.constellation.orion.viewer.selection

import android.content.DialogInterface
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.dialog.DialogOverView
import universe.constellation.orion.viewer.document.TextInfoBuilder
import universe.constellation.orion.viewer.dpToPixels
import universe.constellation.orion.viewer.view.PageLayoutManager
import kotlin.math.max
import kotlin.math.min

class SelectionAutomata(activity: OrionViewerActivity) :
    DialogOverView(activity, R.layout.text_selector, android.R.style.Theme_Translucent_NoTitleBar) {
    private enum class STATE {
        START, MOVING, ACTIVE_SELECTION, MOVING_HANDLER, CANCELED
    }

    private var state = STATE.CANCELED

    private var startX = 0f
    private var startY = 0f
    private var width = 0f
    private var height = 0f

    private val selectionView: SelectionViewNew = dialog.findViewById(R.id.text_selector)

    private var isSingleWord = false

    private var translate = false

    private val radius: Float

    private var activeHandler: Handler? = null

    private var actions: SelectedTextActions? = null

    private var isMovingHandlers = false

    private var builder: TextInfoBuilder? = null

    init {
        selectionView.setOnTouchListener { v: View?, event: MotionEvent ->
            this@SelectionAutomata.onTouch(
                event
            )
        }
        radius = activity.dpToPixels(10f).toFloat()
    }

    fun onTouch(event: MotionEvent): Boolean {
        val action = event.action

        val oldState = state
        var result = true
        when (state) {
            STATE.START -> if (action == MotionEvent.ACTION_DOWN) {
                startX = event.x
                startY = event.y
                width = 0f
                height = 0f
                state = STATE.MOVING
                selectionView.reset()
            } else {
                state = STATE.CANCELED
            }

            STATE.MOVING -> {
                val endX = event.x
                val endY = event.y
                width = endX - startX
                height = endY - startY
                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.ACTIVE_SELECTION
                } else {
                    selectionView.updateView(
                        RectF(
                            min(startX.toDouble(), endX.toDouble()).toFloat(),
                            min(startY.toDouble(), endY.toDouble())
                                .toFloat(),
                            max(startX.toDouble(), endX.toDouble())
                                .toFloat(),
                            max(startY.toDouble(), endY.toDouble()).toFloat()
                        )
                    )
                }
            }

            STATE.ACTIVE_SELECTION -> {
                println("XXX$action")
                if (action == MotionEvent.ACTION_DOWN) {
                    activeHandler =
                        selectionView.findClosestHandler(event.x, event.y, radius * 1.2f)
                    println(activeHandler)
                    if (activeHandler == null) {
                        state = STATE.CANCELED
                        result = false
                    } else {
                        state = STATE.MOVING_HANDLER
                        isMovingHandlers = true
                        isSingleWord = false
                        if (actions != null) actions!!.dismissOnlyDialog()
                    }
                }
            }

            STATE.MOVING_HANDLER -> if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE) {
                activeHandler!!.x = event.x
                activeHandler!!.y = event.y
                selectionView.invalidate()

                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.ACTIVE_SELECTION
                    startX = selectionView.startHandler!!.x
                    width = selectionView.endHandler!!.x - startX

                    startY = selectionView.startHandler!!.y
                    height = selectionView.endHandler!!.y - startY
                }
            } else {
                result = false
            }

            else -> result = false
        }
        if (oldState != state) {
            when (state) {
                STATE.CANCELED -> {
                    actions!!.dismissOnlyDialog()

                    actions = null
                    dialog.dismiss()
                }

                STATE.ACTIVE_SELECTION -> selectText(
                    isSingleWord,
                    translate,
                    selectionRectangle,
                    screenSelectionRect
                )
                else -> {
                }
            }
        }
        return result
    }

    fun selectText(
        isSingleWord: Boolean,
        translate: Boolean,
        data: List<PageAndSelection>,
        originSelection: Rect
    ) {
        var originSelection = originSelection
        val sb = StringBuilder()
        var first = true
        val controller = activity.controller ?: return

        for (selection in data) {
            println(selection.absoluteRectWithoutCrop)
            val rect = selection.absoluteRectWithoutCrop
            val text = controller.selectRawText(
                selection.page,
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                isSingleWord
            )

            if (text != null) {
                if (!first) {
                    sb.append(" ")
                }
                sb.append(text.value)
                first = false
            }
            if ((isSingleWord && text != null) || isMovingHandlers) {
                val originRect = text!!.rect
                val sceneRect = selection.pageView.getSceneRect(originRect)
                originSelection = Rect(
                    sceneRect.left.toInt(),
                    sceneRect.top.toInt(),
                    sceneRect.right.toInt(),
                    sceneRect.bottom.toInt()
                )
                selectionView.updateView(sceneRect)
                builder = text.textInfo
                if (!isMovingHandlers) {
                    selectionView.setHandlers(
                        Handler(
                            originSelection.left - radius / 2,
                            originSelection.top - radius / 2,
                            radius
                        ),
                        Handler(
                            originSelection.right + radius / 2,
                            originSelection.bottom + radius / 2,
                            radius
                        )
                    )
                }
            }
        }
        val text = sb.toString()
        if (!text.isEmpty()) {
            if (isSingleWord && translate) {
                dialog.dismiss()
                Action.DICTIONARY.doAction(controller, activity, text)
            } else {
                val selectedTextActions = SelectedTextActions(activity, dialog)
                actions = selectedTextActions
                if (isSingleWord && !dialog.isShowing) {
                    //TODO: refactor
                    val origin = originSelection
                    dialog.setOnShowListener { dialog2: DialogInterface? ->
                        selectedTextActions.show(text, origin)
                        dialog.setOnShowListener(null)
                    }
                    startSelection(true, false, true)
                    state = STATE.ACTIVE_SELECTION
                } else {
                    selectedTextActions.show(text, originSelection)
                }
            }
        } else {
            dialog.dismiss()
            activity.showFastMessage(R.string.warn_no_text_in_selection)
        }
    }

    @JvmOverloads
    fun startSelection(isSingleWord: Boolean, translate: Boolean, quite: Boolean = false) {
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
        }
        state = STATE.START
        this.isSingleWord = isSingleWord
        this.translate = translate
    }

    private val selectionRectangle: List<PageAndSelection>
        get() {
            val screenRect = screenSelectionRect
            return getSelectionRectangle(
                screenRect.left,
                screenRect.top,
                screenRect.width(),
                screenRect.height(),
                isSingleWord,
                activity.controller!!.pageLayoutManager
            )
        }

    private val screenSelectionRect: Rect
        get() {
            var startX = this.startX
            var startY = this.startY
            var width = this.width
            var height = this.height

            if (width < 0) {
                startX += width
                width = -width
            }
            if (height < 0) {
                startY += height
                height = -height
            }

            return Rect(
                startX.toInt(),
                startY.toInt(),
                (startX + width).toInt(),
                (startY + height).toInt()
            )
        }

    companion object {
        private const val SINGLE_WORD_AREA = 2

        fun getSelectionRectangle(
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            isSingleWord: Boolean,
            pageLayoutManager: PageLayoutManager
        ): List<PageAndSelection> {
            val rect = getSelectionRect(startX, startY, width, height, isSingleWord)
            return pageLayoutManager.findPageAndPageRect(rect)
        }

        fun getSelectionRect(
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            isSingleWord: Boolean
        ): Rect {
            val singleWordDelta = if (isSingleWord) SINGLE_WORD_AREA else 0
            val x = startX - singleWordDelta
            val y = startY - singleWordDelta
            return Rect(x, y, x + width + singleWordDelta, y + height + singleWordDelta)
        }
    }
}
