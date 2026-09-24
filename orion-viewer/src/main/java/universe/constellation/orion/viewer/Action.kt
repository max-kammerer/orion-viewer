package universe.constellation.orion.viewer

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import universe.constellation.orion.viewer.dialog.toDialogMargins
import universe.constellation.orion.viewer.dialog.toMargins
import universe.constellation.orion.viewer.dictionary.openDictionary
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase.Companion.DONT_OPEN_RECENT_FILE
import universe.constellation.orion.viewer.formats.FileFormats
import universe.constellation.orion.viewer.outline.showOutline
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication.Companion.instance
import universe.constellation.orion.viewer.prefs.OrionBookPreferencesActivityX
import universe.constellation.orion.viewer.prefs.OrionPreferenceActivityX
import universe.constellation.orion.viewer.util.ColorUtil.getColorMode
import java.io.File

/** Section of the action picker; [needsBook] says whether the actions of it work on an opened book only. */
enum class ActionGroup(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val needsBook: Boolean = true
) {
    NAVIGATION(R.string.action_group_navigation, R.drawable.new_navigation),
    VIEW(R.string.action_group_view, R.drawable.new_zoom),
    TEXT(R.string.action_group_text, R.drawable.new_select),
    BOOK(R.string.action_group_book, R.drawable.new_book_settings),
    APPLICATION(R.string.action_group_application, R.drawable.new_settings, needsBook = false),
    /** Margin cropping by hardware keys, e-ink readers: the crop dialog is the touch way. Last and folded. */
    CROP_KEYS(R.string.action_group_crop_keys, R.drawable.new_cut)
}

/**
 * [group] is null for [NONE] only, which the picker shows first on its own. The activity runs an
 * action of a group with [ActionGroup.needsBook] only while a book is open, so an action started
 * between books, e.g. from a tap during a slow opening, is a no-op.
 */
enum class Action(@StringRes val nameRes: Int, @IntegerRes idRes: Int, val group: ActionGroup?) {
    NONE(R.string.action_none, R.integer.action_none, null) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            //none action
        }
    },

    MENU(R.string.action_menu, R.integer.action_menu, ActionGroup.APPLICATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showMenu()
        }
    },

    NEXT(R.string.action_next_page, R.integer.action_next_page, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawNext()
        }
    },

    PREV(R.string.action_prev_page, R.integer.action_prev_page, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.drawPrev()
        }
    },

    NEXT10(R.string.action_next_10, R.integer.action_next_10, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val controller1 = controller ?:  return
            var page = controller1.currentPage + controller1.docPagesFor(10)

            if (page > controller1.pageCount - 1) {
                page = controller1.pageCount - 1
            }
            controller1.goToPage(page, NavKind.STEP)
        }
    },

    PREV10(R.string.action_prev_10, R.integer.action_prev_10, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val controller1 = controller ?: return
            var page = controller1.currentPage - controller1.docPagesFor(10)

            if (page < 0) {
                page = 0
            }
            controller1.goToPage(page, NavKind.STEP)
        }
    },

    FIRST_PAGE(R.string.action_first_page, R.integer.action_first_page, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.goToPage(0)
        }
    },

    LAST_PAGE(R.string.action_last_page, R.integer.action_last_page, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.goToPage(controller.pageCount - 1)
        }
    },

    NAVIGATE_BACK(R.string.action_navigate_back, R.integer.action_navigate_back, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.navigateBack()
        }
    },

    NAVIGATE_FORWARD(R.string.action_navigate_forward, R.integer.action_navigate_forward, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.navigateForward()
        }
    },

/** Flips the "Enable touch move" option: the touch processor reads it on every gesture. */
    SWITCH_TOUCH_MOVE(R.string.action_switch_touch_move, R.integer.action_switch_touch_move, ActionGroup.APPLICATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val options = activity.globalOptions
            val enable = !options.ENABLE_TOUCH_MOVE.value
            options.saveBooleanProperty(GlobalOptions.ENABLE_TOUCH_MOVE, enable)
            activity.showFastMessage(if (enable) R.string.msg_touch_move_enabled else R.string.msg_touch_move_disabled)
        }
    },

    SHOW_OUTLINE(R.string.action_outline, R.integer.action_open_outline, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            log("Show Outline...")
            controller?.let {
                showOutline(it, activity)
            }
        }
    },

    SEARCH(R.string.action_search, R.integer.action_search, ActionGroup.TEXT) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.startSearch()
        }
    },

    SELECT_TEXT(R.string.action_select_text, R.integer.action_select_text, ActionGroup.TEXT) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(false, false)
        }
    },

    SELECT_WORD(R.string.action_select_word, R.integer.action_select_word, ActionGroup.TEXT) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(true, false)
        }
    },

    SELECT_WORD_AND_TRANSLATE(R.string.action_select_word_and_translate, R.integer.action_select_word_and_translate, ActionGroup.TEXT) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.textSelectionMode(true, true)
        }
    },

    ADD_BOOKMARK(R.string.action_add_bookmark, R.integer.action_add_bookmark, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.ADD_BOOKMARK_SCREEN, this, parameter)
        }
    },

    OPEN_BOOKMARKS(R.string.action_open_bookmarks, R.integer.action_open_bookmarks, ActionGroup.NAVIGATION) {
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

    FULL_SCREEN(R.string.action_full_screen, R.integer.action_full_screen, ActionGroup.APPLICATION) {
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

    SWITCH_COLOR_MODE(R.string.action_switch_color_mode, R.integer.action_switch_color_mode, ActionGroup.VIEW) {
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

    BOOK_OPTIONS(R.string.action_book_options, R.integer.action_book_options, ActionGroup.BOOK) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionBookPreferencesActivityX::class.java)
            activity.startActivity(intent)
        }
    },

    ZOOM(R.string.action_zoom_page, R.integer.action_zoom_page, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.ZOOM_SCREEN, null, null)
        }
    },

    PAGE_LAYOUT(R.string.action_layout_page, R.integer.action_page_layout, ActionGroup.VIEW) {
        override fun doAction(activity: OrionBaseActivity) {
            BOOK_OPTIONS.doAction(activity)
        }
    },

    CROP(R.string.action_crop_page, R.integer.action_crop_page, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.CROP_SCREEN, null, null)
        }
    },

    GOTO(R.string.action_goto_page, R.integer.action_goto_page, ActionGroup.NAVIGATION) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            activity.showOrionDialog(OrionViewerActivity.PAGE_SCREEN, this, null)
        }
    },

    ROTATION(R.string.action_rotation_page, R.integer.action_rotation_page, ActionGroup.VIEW) {
        override fun doAction(activity: OrionBaseActivity) {
            BOOK_OPTIONS.doAction(activity)
        }
    },

    DICTIONARY(R.string.action_dictionary, R.integer.action_dictionary, ActionGroup.TEXT) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val dict = when (val bookDict = activity.lastPageInfo?.dictionary ?: "DEFAULT") {
                "DEFAULT" -> activity.globalOptions.defaultDictionary
                else -> bookDict
            }

            openDictionary(parameter as? String?, activity, dict)
        }
    },

    OPEN_BOOK(R.string.action_open, R.integer.action_open_book, ActionGroup.APPLICATION) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionFileManagerActivity::class.java)
            intent.putExtra(DONT_OPEN_RECENT_FILE, true)
            activity.startActivity(intent)
        }
    },

    OPTIONS(R.string.action_options_page, R.integer.action_options_page, ActionGroup.APPLICATION) {
        override fun doAction(activity: OrionBaseActivity) {
            val intent = Intent(activity, OrionPreferenceActivityX::class.java)
            activity.startActivity(intent)
        }
    },

    CLOSE_ACTION(R.string.action_close, R.integer.action_close, ActionGroup.APPLICATION) {
        override fun doAction(activity: OrionBaseActivity) {
            activity.finish()
        }
    },

    SHARE_FILE(R.string.menu_share_file, R.integer.action_share_file, ActionGroup.BOOK) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            val path = controller?.document?.filePath ?: return
            val file = File(path)
            if (file.exists()) {
                val uri = try {
                    androidx.core.content.FileProvider.getUriForFile(
                        activity,
                        activity.applicationContext.packageName + ".fileprovider",
                        file
                    )
                } catch (e: IllegalArgumentException) {
                    //the file is outside the provider roots (file_paths.xml)
                    activity.analytics.error(e, "share: $path")
                    activity.showWarning(R.string.msg_share_file_failed)
                    return
                }
                val intent = Intent(Intent.ACTION_SEND)

                val fileExt = file.name.lowercase().substringAfterLast(".")
                intent.type = FileFormats.getMimeTypeFromExtension(fileExt) ?: "application/pdf"

                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.menu_share_file)))
            }
        }
    },

    FIT_WIDTH(R.string.action_fit_width, R.integer.action_fit_width, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.changeZoom(0)
        }
    },

    FIT_HEIGHT(R.string.action_fit_height, R.integer.action_fit_heigh, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.changeZoom(-1)
        }
    },

    FIT_PAGE(R.string.action_fit_page, R.integer.action_fit_page, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            controller?.changeZoom(-2)
        }
    },

    ROTATE_90(R.string.action_rotate_90, R.integer.action_rotate_90, ActionGroup.VIEW) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            //controller.setRotation((controller.getRotation() - 1) % 2);
            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                controller?.changeOrinatation("PORTRAIT")
            } else {
                controller?.changeOrinatation("LANDSCAPE")
            }
        }
    },

    ROTATE_270(R.string.action_rotate_270, R.integer.action_rotate_270, ActionGroup.VIEW) {
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
                controller?.changeOrinatation("LANDSCAPE_INVERSE")
            }
        }
    },

    INVERSE_CROP(R.string.action_inverse_crops, R.integer.action_inverse_crop, ActionGroup.CROP_KEYS) {
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

    SWITCH_CROP(R.string.action_switch_long_crop, R.integer.action_switch_long_crop, ActionGroup.CROP_KEYS) {
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

    CROP_LEFT(R.string.action_crop_left, R.integer.action_crop_left, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, true, 0)
        }
    },

    UNCROP_LEFT(R.string.action_uncrop_left, R.integer.action_uncrop_left, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, false, 0)
        }
    },

    CROP_RIGHT(R.string.action_crop_right, R.integer.action_crop_right, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, true, 1)
        }
    },

    UNCROP_RIGHT(R.string.action_uncrop_right, R.integer.action_uncrop_right, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, false, 1)
        }
    },

    CROP_TOP(R.string.action_crop_top, R.integer.action_crop_top, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, true, 2)
        }
    },

    UNCROP_TOP(R.string.action_uncrop_top, R.integer.action_uncrop_top, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, false, 2)
        }
    },

    CROP_BOTTOM(R.string.action_crop_bottom, R.integer.action_crop_bottom, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, true, 3)
        }
    },

    UNCROP_BOTTOM(R.string.action_uncrop_bottom, R.integer.action_uncrop_bottom, ActionGroup.CROP_KEYS) {
        override fun doAction(
            controller: Controller?,
            activity: OrionViewerActivity,
            parameter: Any?
        ) {
            updateMargin(controller ?: return, false, 3)
        }
    };

    @JvmField
    val code: Int = instance.resources.getInteger(idRes)

    val needsBook: Boolean
        get() = group?.needsBook ?: false

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
                //codes are static resources, so a collision is always a build time bug:
                //the later action silently shadows the earlier one in every action list
                val shadowed = actions.put(value.code, value)
                if (shadowed != null) {
                    error("Actions $shadowed and $value share code ${value.code}")
                }
            }
        }

        @JvmStatic
        fun getAction(code: Int): Action {
            val result = actions[code]
            return result ?: NONE
        }
    }
}
