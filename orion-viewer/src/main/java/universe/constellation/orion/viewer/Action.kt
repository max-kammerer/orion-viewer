package universe.constellation.orion.viewer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import universe.constellation.orion.viewer.dialog.toDialogMargins
import universe.constellation.orion.viewer.dialog.toMargins
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase.Companion.DONT_OPEN_RECENT_FILE
import universe.constellation.orion.viewer.outline.showOutline
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication.Companion.instance
import universe.constellation.orion.viewer.prefs.OrionBookPreferencesActivityX
import universe.constellation.orion.viewer.prefs.OrionPreferenceActivityX
import universe.constellation.orion.viewer.util.ColorUtil.getColorMode

enum class Action(@StringRes val nameRes: Int, @IntegerRes idRes: Int, val isVisible: Boolean = true) {
    NONE(R.string.action_none, R.integer.action_none) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            //none action
        }
    },

    MENU(R.string.action_menu, R.integer.action_menu) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showMenu()
        }
    },


    NEXT(R.string.action_next_page, R.integer.action_next_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawNext()
        }
    },

    PREV(R.string.action_prev_page, R.integer.action_prev_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawPrev()
        }
    },

    NEXT10(R.string.action_next_10, R.integer.action_next_10) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            var page = controller!!.currentPage + 10

            if (page > controller.pageCount - 1) {
                page = controller.pageCount - 1
            }
            controller.drawPage(page)
        }
    },


    PREV10(R.string.action_prev_10, R.integer.action_prev_10) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            var page = controller!!.currentPage - 10

            if (page < 0) {
                page = 0
            }
            controller.drawPage(page)
        }
    },

    FIRST_PAGE(R.string.action_first_page, R.integer.action_first_page, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawPage(0)
        }
    },

    LAST_PAGE(R.string.action_last_page, R.integer.action_last_page, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawPage(controller.pageCount - 1)
        }
    },

    SHOW_OUTLINE(R.string.action_outline, R.integer.action_open_outline) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            log("Show Outline...")
            showOutline(controller!!, activity)
        }
    },

    SEARCH(R.string.action_crop_page, R.integer.action_crop_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.startSearch()
        }
    },

    SELECT_TEXT(R.string.action_select_text, R.integer.action_select_text) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(false, false)
        }
    },

    SELECT_WORD(R.string.action_select_word, R.integer.action_select_word) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(true, false)
        }
    },

    SELECT_WORD_AND_TRANSLATE(
        R.string.action_select_word_and_translate,
        R.integer.action_select_word_and_translate
    ) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(true, true)
        }
    },

    ADD_BOOKMARK(R.string.action_add_bookmark, R.integer.action_add_bookmark) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.ADD_BOOKMARK_SCREEN, this, parameter)
        }
    },

    OPEN_BOOKMARKS(R.string.action_open_bookmarks, R.integer.action_open_bookmarks) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val bookmark = Intent(activity.applicationContext, OrionBookmarkActivity::class.java)
            bookmark.putExtra(OrionBookmarkActivity.BOOK_ID, activity.bookId)
            activity.startActivityForResult(
                bookmark,
                OrionViewerActivity.OPEN_BOOKMARK_ACTIVITY_RESULT
            )
        }
    },

    FULL_SCREEN(R.string.action_full_screen, R.integer.action_full_screen) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val options = activity.globalOptions
            options.saveBooleanProperty(
                GlobalOptions.FULL_SCREEN,
                java.lang.Boolean.FALSE == options.FULL_SCREEN.value
            )
        }
    },

    SWITCH_COLOR_MODE(R.string.action_switch_color_mode, R.integer.action_switch_color_mode) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val view = activity.view
            val scene = activity.fullScene
            val currentBookParameters = activity.orionApplication.currentBookParameters
            if (currentBookParameters != null && getColorMode(currentBookParameters.colorMode) == null) {
                activity.showLongMessage(activity.getString(R.string.select_color_mode))
                return
            }
            if (view.isDefaultColorMatrix()) {
                if (currentBookParameters != null) {
                    scene.setColorMatrix(getColorMode(currentBookParameters.colorMode))
                }
            } else {
                scene.setColorMatrix(null)
            }
            view.invalidate()
        }
    },

    BOOK_OPTIONS(R.string.action_book_options, R.integer.action_book_options) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionBookPreferencesActivityX::class.java)
            activity.startActivity(intent)
        }
    },

    ZOOM(R.string.action_zoom_page, R.integer.action_zoom_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.ZOOM_SCREEN, null, null)
        }
    },

    PAGE_LAYOUT(R.string.action_layout_page, R.integer.action_page_layout) {
        override fun doAction(activity: OrionBaseActivity) {
            BOOK_OPTIONS.doAction(activity)
        }
    },

    CROP(R.string.action_crop_page, R.integer.action_crop_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.CROP_SCREEN, null, null)
        }
    },

    GOTO(R.string.action_goto_page, R.integer.action_goto_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.PAGE_SCREEN, this, null)
        }
    },

    ROTATION(R.string.action_rotation_page, R.integer.action_rotation_page) {
        override fun doAction(activity: OrionBaseActivity) {
            BOOK_OPTIONS.doAction(activity)
        }
    },

    DICTIONARY(R.string.action_dictionary, R.integer.action_dictionary) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            var parameter = parameter
            val dict = activity.globalOptions.dictionary
            var action: String? = null
            val intent = Intent()
            var queryText: String? = null

            when (dict) {
                "FORA" -> {
                    action = "com.ngc.fora.action.LOOKUP"
                    queryText = "HEADWORD"
                }
                "COLORDICT" -> {
                    action = "colordict.intent.action.SEARCH"
                    queryText = "EXTRA_QUERY"
                }
                "AARD" -> {
                    action = Intent.ACTION_SEARCH
                    intent.setClassName("aarddict.android", "aarddict.android.LookupActivity")
                    queryText = "query"
                    parameter = safeParameter(parameter)
                }
                "AARD2" -> {
                    action = "aard2.lookup"
                    queryText = "query"
                    parameter = safeParameter(parameter)
                }
                "LINGVO" -> {
                    action = "com.abbyy.mobile.lingvo.intent.action.TRANSLATE"
                    intent.setPackage("com.abbyy.mobile.lingvo.market")
                    queryText = "com.abbyy.mobile.lingvo.intent.extra.TEXT"
                    parameter = safeParameter(parameter)
                }
            }

            if (action != null) {
                intent.setAction(action)
                if (parameter != null) {
                    intent.putExtra(queryText, parameter as String?)
                }

                try {
                    activity.startActivity(intent)
                } catch (ex: ActivityNotFoundException) {
                    log(ex)
                    val string = activity.getString(R.string.warn_msg_no_dictionary)
                    activity.showWarning(string + ": " + dict + ": " + ex.message)
                }
            }
        }

        private fun safeParameter(parameter: Any?): Any {
            return parameter ?: ""
        }
    },

    OPEN_BOOK(R.string.action_open, R.integer.action_open_book) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionFileManagerActivity::class.java)
            intent.putExtra(DONT_OPEN_RECENT_FILE, true)
            activity.startActivity(intent)
        }
    },

    OPTIONS(R.string.action_options_page, R.integer.action_options_page) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionPreferenceActivityX::class.java)
            activity.startActivity(intent)
        }
    },

    CLOSE_ACTION(R.string.action_close, R.integer.action_close, isVisible = false) {
        override fun doAction(activity: OrionBaseActivity) {
            activity.finish()
        }
    },

    FIT_WIDTH(R.string.action_fit_width, R.integer.action_fit_width) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller!!.changeZoom(0)
        }
    },

    FIT_HEIGHT(R.string.action_fit_height, R.integer.action_fit_heigh) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller!!.changeZoom(-1)
        }
    },

    FIT_PAGE(R.string.action_fit_page, R.integer.action_fit_page) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller!!.changeZoom(-2)
        }
    },


    ROTATE_90(R.string.action_rotate_90, R.integer.action_rotate_90) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            //controller.setRotation((controller.getRotation() - 1) % 2);
            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                controller!!.changeOrinatation("PORTRAIT")
            } else {
                controller!!.changeOrinatation("LANDSCAPE")
            }
        }
    },

    ROTATE_270(R.string.action_rotate_270, R.integer.action_rotate_270) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            //controller.setRotation((controller.getRotation() + 1) % 2);
            val isLevel9 = activity.orionApplication.sdkVersion >= 9
            if (!isLevel9 || activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                ROTATE_90.doAction(controller, activity, parameter)
            } else {
                controller!!.changeOrinatation("LANDSCAPE_INVERSE")
            }
        }
    },


    INVERSE_CROP(R.string.action_inverse_crops, R.integer.action_inverse_crop) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val opts = activity.orionApplication.tempOptions
            opts!!.inverseCropping = !opts.inverseCropping

            val title =
                activity.resources.getString(R.string.action_inverse_crops) + ":" + (if (opts.inverseCropping) "inverted" else "normal")
            Toast.makeText(activity.applicationContext, title, Toast.LENGTH_SHORT).show()
        }
    },

    SWITCH_CROP(R.string.action_switch_long_crop, R.integer.action_switch_long_crop) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val opts = activity.orionApplication.tempOptions
            opts!!.switchCropping = !opts.switchCropping
            val title =
                activity.resources.getString(R.string.action_switch_long_crop) + ":" + (if (opts.switchCropping) "big" else "small")
            Toast.makeText(activity.applicationContext, title, Toast.LENGTH_SHORT).show()
        }
    },

    CROP_LEFT(R.string.action_crop_left, R.integer.action_crop_left, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, true, 0)
        }
    },

    UNCROP_LEFT(R.string.action_uncrop_left, R.integer.action_uncrop_left, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, false, 0)
        }
    },

    CROP_RIGHT(R.string.action_crop_right, R.integer.action_crop_right, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, true, 1)
        }
    },

    UNCROP_RIGHT(R.string.action_uncrop_right, R.integer.action_uncrop_right, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, false, 1)
        }
    },

    CROP_TOP(R.string.action_crop_top, R.integer.action_crop_top, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, true, 2)
        }
    },

    UNCROP_TOP(R.string.action_uncrop_top, R.integer.action_uncrop_top, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, false, 2)
        }
    },

    CROP_BOTTOM(R.string.action_crop_bottom, R.integer.action_crop_bottom, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, true, 3)
        }
    },

    UNCROP_BOTTOM(R.string.action_uncrop_bottom, R.integer.action_uncrop_bottom, isVisible = false) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller!!, false, 3)
        }
    };

    @JvmField
    val code: Int = instance.resources.getInteger(idRes)

    open fun doAction(controller: Controller?, activity: OrionViewerActivity, parameter: Any?) {
        doAction(activity)
    }

    open fun doAction(activity: OrionBaseActivity) {
    }

    protected fun updateMargin(controller: Controller, isCrop: Boolean, index: Int) {
        var isCrop = isCrop
        var index = index
        val cropMargins = controller.margins
        if (cropMargins.evenCrop && controller.isEvenPage) {
            if (index == 0 || index == 1) {
                index += 4
            }
        }

        val margins = cropMargins.toDialogMargins()
        val context = controller.activity.orionApplication
        val tempOpts = context.tempOptions
        if (tempOpts!!.inverseCropping) {
            isCrop = !isCrop
        }
        val delta = if (tempOpts.switchCropping) context.options.longCrop else 1
        margins[index] += if (isCrop) delta else -delta
        if (margins[index] > OrionViewerActivity.CROP_RESTRICTION_MAX) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MAX
        }
        if (margins[index] < OrionViewerActivity.CROP_RESTRICTION_MIN) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MIN
        }

        controller.changeCropMargins(
            margins.toMargins(cropMargins.evenCrop, cropMargins.cropMode)
        )
    }

    fun getActionName(context: Context): String {
        return context.getString(nameRes)
    }

    companion object {
        private val actions = HashMap<Int, Action>()

        init {
            val values = entries.toTypedArray()
            for (value in values) {
                actions[value.code] = value
            }
        }

        @JvmStatic
        fun getAction(code: Int): Action {
            val result = actions[code]
            return result ?: NONE
        }
    }
}
