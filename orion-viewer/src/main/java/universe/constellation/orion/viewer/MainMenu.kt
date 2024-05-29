package universe.constellation.orion.viewer

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainMenu(private val mainMenu: View, val orionViewerActivity: OrionViewerActivity) {

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

        pageSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                orionViewerActivity.controller?.let {
                    controller ->
                    if (fromUser) {
                        controller.drawPage(progress)
                    }
                    curPage.setGotoSpannable((progress + 1).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mainMenu.findViewById<View>(R.id.menu_top_actions).setOnClickListener {}
        mainMenu.findViewById<View>(R.id.menu_botton_actions_all).setOnClickListener {}
        mainMenu.findViewById<ImageView>(R.id.page_picker_minus).setOnClickListener {
            if (pageSeeker.progress - 1 >= 0) {
                pageSeeker.progress -= 1
                orionViewerActivity.controller?.drawPage(pageSeeker.progress)
            }
        }
        mainMenu.findViewById<ImageView>(R.id.page_picker_plus).setOnClickListener {
            if (pageSeeker.progress + 1 <= pageSeeker.max) {
                pageSeeker.progress += 1
                orionViewerActivity.controller?.drawPage(pageSeeker.progress)
            }
        }
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
            orionViewerActivity.doMenuAction(viewId)
        }
    }

    fun hideMenu() {
        mainMenu.visibility = View.INVISIBLE
    }


    fun showMenu() {
        val controller = orionViewerActivity.controller
        if (controller != null) {
            pageSeeker.max = controller.pageCount - 1
            pageSeeker.progress = controller.currentPage
            pageCount.setGotoSpannable(controller.pageCount.toString())
        } else {
            pageSeeker.max = 1
            pageSeeker.progress = 1
            pageCount.text = "1"
        }
        mainMenu.visibility = View.VISIBLE
    }

    private fun TextView.setGotoSpannable(text: String) {
        val spannable = SpannableStringBuilder(text)
        val onClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                hideMenu()
                orionViewerActivity.doMenuAction(R.id.goto_menu_item)
            }
        }
        spannable.setSpan(onClick, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.text = spannable
        this.movementMethod = LinkMovementMethod.getInstance();
    }
}