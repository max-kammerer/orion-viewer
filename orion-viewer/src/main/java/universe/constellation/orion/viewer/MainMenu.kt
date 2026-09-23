package universe.constellation.orion.viewer

import android.graphics.Paint
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainMenu(private val mainMenu: View, val activity: OrionViewerActivity) {

    private val pageSeeker = mainMenu.findViewById<SeekBar>(R.id.page_picker_seeker)!!
    private val pageCount = mainMenu.findViewById<TextView>(R.id.page_count)!!
    private val curPage = mainMenu.findViewById<TextView>(R.id.cur_page)!!
    private val topExtraPanel = mainMenu.findViewById<ViewGroup>(R.id.menu_top_extra)!!

    init {
        initImageViewActions(mainMenu, R.id.menu_top_actions)
        initImageViewActions(topExtraPanel)
        initImageViewActions(mainMenu, R.id.menu_botton_actions)

        mainMenu.findViewById<LinearLayout>(R.id.menu_middle_part).setOnClickListener {
            hideMenu()
        }

        mainMenu.findViewById<View>(R.id.menu_top_actions).setOnClickListener {}
        mainMenu.findViewById<View>(R.id.menu_botton_actions_all).setOnClickListener {}

        //plain click listeners: a ClickableSpan needs LinkMovementMethod, which makes the view
        //focusable, and on Android 7 taking focus before the first layout crashes in the method
        for (pageLink in listOf(curPage, pageCount)) {
            pageLink.paintFlags = pageLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            pageLink.setOnClickListener {
                hideMenu()
                activity.doMenuAction(R.id.goto_menu_item)
            }
        }

        val minus = mainMenu.findViewById<ImageView>(R.id.page_picker_minus)
        val plus = mainMenu.findViewById<ImageView>(R.id.page_picker_plus)
        initPageNavControls(activity, pageSeeker, minus, plus, curPage, PageNumbering::sheetLabel)
    }

    private fun initImageViewActions(view: View, id: Int) {
        val panel = view.findViewById<ViewGroup>(id)
        initImageViewActions(panel)
    }

    private fun initImageViewActions(panel: ViewGroup) {
        for (i in 0 until panel.childCount) {
            val child = panel.getChildAt(i)
            if (child is ImageView) {
                child.setOnClickListener {
                    processClick(it.id)
                }
            }
        }
    }

    private fun processClick(viewId: Int) {
        if (viewId == R.id.more_menu_item) {
            val newVisibility =
                when(topExtraPanel.visibility) {
                    VISIBLE -> INVISIBLE
                    else -> VISIBLE
                }
            topExtraPanel.visibility = newVisibility
        } else {
            hideMenu()
            activity.doMenuAction(viewId)
        }
    }

    fun hideMenu() {
        mainMenu.visibility = View.INVISIBLE
    }


    fun showMenu() {
        val controller = activity.controller
        initPageNavigationValues(controller, pageSeeker, pageCount)
        mainMenu.visibility = VISIBLE
    }
}

fun initPageNavigationValues(
    controller: Controller?,
    pageSeeker: SeekBar,
    pageCount: TextView,
    setTextAction: TextView.(lastPageLabel: String) -> Unit = { text = it }
) {
    if (controller != null) {
        pageSeeker.max = controller.pageCount - 1
        pageSeeker.progress = controller.currentPage
        val numbering = controller.pageNumbering
        pageCount.setTextAction(numbering.label(numbering.lastOn(controller.pageCount - 1)))
    } else {
        pageSeeker.max = 1
        pageSeeker.progress = 1
        pageCount.text = "1"
    }
}

fun initPageNavControls(
    activity: OrionViewerActivity,
    pageSeeker: SeekBar,
    minus: View,
    plus: View,
    curPage: TextView,
    /** The text for the 0-based document page under the seeker; an editable field takes a single number. */
    pageLabel: PageNumbering.(docPage: Int) -> String = { label(firstOn(it)) },
    setTextAction: TextView.(pageLabel: String) -> Unit = {
        text = it
    }
) {
    pageSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                openPage(activity.controller, progress)
            }
            val numbering = activity.controller?.pageNumbering ?: PageNumbering()
            curPage.setTextAction(numbering.pageLabel(progress))
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })


    minus.setOnClickListener {
        if (pageSeeker.progress - 1 >= 0) {
            pageSeeker.progress -= 1
            openPage(activity.controller, pageSeeker.progress)
        }
    }

    plus.setOnClickListener {
        if (pageSeeker.progress + 1 <= pageSeeker.max) {
            pageSeeker.progress += 1
            openPage(activity.controller, pageSeeker.progress)
        }
    }
}

private fun openPage(controller: Controller?, pageNum: Int) {
    controller?.goToPage(pageNum)
}