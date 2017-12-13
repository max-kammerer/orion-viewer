/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeViewList;
import universe.constellation.orion.viewer.dialog.CropDialogBuilderKt;
import universe.constellation.orion.viewer.document.OutlineItem;
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity;
import universe.constellation.orion.viewer.layout.CropMargins;
import universe.constellation.orion.viewer.outline.OutlineAdapter;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.prefs.OrionApplication;
import universe.constellation.orion.viewer.prefs.OrionBookPreferences;
import universe.constellation.orion.viewer.prefs.OrionPreferenceActivity;
import universe.constellation.orion.viewer.prefs.TemporaryOptions;
import universe.constellation.orion.viewer.util.ColorUtil;
import universe.constellation.orion.viewer.view.FullScene;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 06.01.12
 * Time: 18:28
 */
public enum Action {

    NONE (R.string.action_none, R.integer.action_none) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            //none action
        }
    } ,

    MENU (R.string.action_menu, R.integer.action_menu) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.getSupportActionBar().openOptionsMenu();
        }
    } ,


    NEXT (R.string.action_next_page, R.integer.action_next_page) {
            @Override
            public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
                if (controller != null) {
                    controller.drawNext();
                }
            }
        } ,

    PREV (R.string.action_prev_page, R.integer.action_prev_page) {
                @Override
                public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
                    if (controller != null) {
                        controller.drawPrev();
                    }
                }
            } ,

    NEXT10 (R.string.action_next_10, R.integer.action_next_10) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            int page = controller.getCurrentPage() + 10;

            if (page > controller.getPageCount() - 1) {
                page = controller.getPageCount() - 1;
            }
            controller.drawPage(page);
        }
    },


    PREV10 (R.string.action_prev_10, R.integer.action_prev_10) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            int page = controller.getCurrentPage() - 10;

            if (page < 0) {
                page = 0;
            }
            controller.drawPage(page);
        }
    },

    FIRST_PAGE (R.string.action_first_page, R.integer.action_first_page) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            if (controller != null) {
                controller.drawPage(0);
            }
        }
    } ,

    LAST_PAGE (R.string.action_last_page, R.integer.action_last_page) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            if (controller != null) {
                controller.drawPage(controller.getPageCount() - 1);
            }
        }
    } ,

    SHOW_OUTLINE (R.string.action_outline, R.integer.action_open_outline) {
		public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            log("Show Outline...");
            OutlineItem[] outline = controller.getOutline();

			if (outline != null && outline.length != 0) {
                final AppCompatDialog dialog = new AppCompatDialog(activity);
                dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.outline);

                final Toolbar toolbar = (Toolbar) dialog.findViewById(R.id.toolbar);
                toolbar.setTitle(R.string.menu_outline_text);
                toolbar.setLogo(R.drawable.collapsed);

                final InMemoryTreeStateManager<Integer> manager = new InMemoryTreeStateManager<>();
                manager.setVisibleByDefault(false);
                int navigateTo = OutlineAdapter.initializeTreeManager(manager, outline, controller.getCurrentPage());
                TreeViewList tocTree = (TreeViewList) dialog.findViewById(R.id.mainTreeView);
                tocTree.setDivider(activity.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));

                tocTree.setAdapter(new OutlineAdapter(controller, activity, dialog, manager, outline));
                tocTree.setSelection(navigateTo);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

                toolbar.setOnClickListener(new View.OnClickListener() {

                    boolean expanded = false;

                    @Override
                    public void onClick(View v) {
                        if (expanded) {
                            toolbar.setLogo(R.drawable.collapsed);
                            List<Integer> children = manager.getChildren(null);
                            for (Integer child : children) {
                                manager.collapseChildren(child);
                            }
                        } else {
                            toolbar.setLogo(R.drawable.expanded);
                            manager.expandEverythingBelow(null);
                        }

                        expanded = !expanded;
                    }
                });
            } else {
                activity.showWarning(R.string.warn_no_outline);
            }
        }
    },

    SEARCH (R.string.action_crop_page, R.integer.action_crop_page) {
                @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {

            activity.startSearch();
        }
    },

    SELECT_TEXT (R.string.action_select_text, R.integer.action_select_text) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.textSelectionMode(false, false);
        }
    },

    SELECT_WORD (R.string.action_select_word, R.integer.action_select_word) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.textSelectionMode(true, false);
        }
    },

    SELECT_WORD_AND_TRANSLATE (R.string.action_select_word_and_translate, R.integer.action_select_word_and_translate) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.textSelectionMode(true, true);
        }
    },

    ADD_BOOKMARK (R.string.action_add_bookmark, R.integer.action_add_bookmark) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.ADD_BOOKMARK_SCREEN, this, parameter);
        }
    },

    OPEN_BOOKMARKS (R.string.action_open_bookmarks, R.integer.action_open_bookmarks) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            Intent bookmark = new Intent(activity.getApplicationContext(), OrionBookmarkActivity.class);
            bookmark.putExtra(OrionBookmarkActivity.BOOK_ID, activity.getBookId());
            activity.startActivityForResult(bookmark, OrionViewerActivity.OPEN_BOOKMARK_ACTIVITY_RESULT);
        }
    },

    FULL_SCREEN (R.string.action_full_screen, R.integer.action_full_screen) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            GlobalOptions options = activity.getGlobalOptions();
            options.saveBooleanProperty(GlobalOptions.FULL_SCREEN, !options.isFullScreen());
        }
    },

    SWITCH_COLOR_MODE (R.string.action_switch_color_mode, R.integer.action_switch_color_mode) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            OrionScene view = activity.getView();
            FullScene scene = activity.getFullScene();
            LastPageInfo currentBookParameters = activity.getOrionContext().getCurrentBookParameters();
            if (currentBookParameters != null && ColorUtil.getColorMode(currentBookParameters.colorMode) == null) {
                activity.showLongMessage(activity.getString(R.string.select_color_mode));
                return;
            }
            if (view.isDefaultColorMatrix()) {
                if (currentBookParameters != null) {
                    scene.setColorMatrix(ColorUtil.getColorMode(currentBookParameters.colorMode));
                }
            } else {
                scene.setColorMatrix(null);
            }
            view.invalidate();
        }
    },

    BOOK_OPTIONS (R.string.action_book_options, R.integer.action_book_options) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            Intent intent = new Intent(activity, OrionBookPreferences.class);
            activity.startActivity(intent);
        }
    },

    ZOOM (R.string.action_zoom_page, R.integer.action_zoom_page) {
            @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.ZOOM_SCREEN, null, null);
        }
    },

    PAGE_LAYOUT (R.string.action_layout_page, R.integer.action_page_layout) {
            @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.PAGE_LAYOUT_SCREEN, null, null);
        }
    },

    CROP (R.string.action_crop_page, R.integer.action_crop_page) {
            @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.CROP_SCREEN, null, null);
        }
    },

    GOTO (R.string.action_goto_page, R.integer.action_goto_page) {

        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.PAGE_SCREEN, this, null);
        }
    },

    ROTATION (R.string.action_rotation_page, R.integer.action_rotation_page)  {
            @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.showOrionDialog(OrionViewerActivity.ROTATION_SCREEN, null, null);
        }
    },

    DICTIONARY (R.string.action_dictionary, R.integer.action_dictionary) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            String dict = activity.getGlobalOptions().getDictionary();
            String action = null;
            Intent intent = new Intent();
            String queryText = null;

            if ("FORA".equals(dict)) {
                action = "com.ngc.fora.action.LOOKUP";
                queryText = "HEADWORD";
            } else if ("COLORDICT".equals(dict)) {
                action = "colordict.intent.action.SEARCH";
                queryText = "EXTRA_QUERY";
            } else if ("AARD".equals(dict)) {
                action = Intent.ACTION_SEARCH;
                intent.setClassName("aarddict.android", "aarddict.android.LookupActivity");
                queryText = "query";
                parameter = safeParameter(parameter);
            } else if ("AARD2".equals(dict)) {
                action = "aard2.lookup";
                queryText = "query";
                parameter = safeParameter(parameter);
            }  else if ("LINGVO".equals(dict)) {
                action = "com.abbyy.mobile.lingvo.intent.action.TRANSLATE";
                intent.setPackage("com.abbyy.mobile.lingvo.market");
                queryText = "com.abbyy.mobile.lingvo.intent.extra.TEXT";
                parameter = safeParameter(parameter);
            }

            if (action != null) {
                intent.setAction(action);
                if (parameter != null) {
                    intent.putExtra(queryText, (String) parameter);
                }

                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    log(ex);
                    String string = activity.getString(R.string.warn_msg_no_dictionary);
                    activity.showWarning(string + ": " + dict + ": " + ex.getMessage());
                }
            }
        }

        private Object safeParameter(Object parameter) {
            return parameter == null ? "" : parameter;
        }
    },

    OPEN_BOOK (R.string.action_open, R.integer.action_open_book) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            Intent intent = new Intent(activity, OrionFileManagerActivity.class);
            intent.putExtra(OrionBaseActivity.DONT_OPEN_RECENT, true);
            activity.startActivity(intent);
        }
    },

    OPTIONS (R.string.action_options_page, R.integer.action_options_page) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            Intent intent = new Intent(activity, OrionPreferenceActivity.class);
            activity.startActivity(intent);
        }
    },

    CLOSE_ACTION (R.string.action_close, R.integer.action_close) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.finish();
        }
    },

    FIT_WIDTH (R.string.action_fit_width, R.integer.action_fit_width) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            controller.changeZoom(0);
        }
    },

    FIT_HEIGHT (R.string.action_fit_height, R.integer.action_fit_heigh) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            controller.changeZoom(-1);
        }
    },

    FIT_PAGE (R.string.action_fit_page, R.integer.action_fit_page) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            controller.changeZoom(-2);
        }
    },


    ROTATE_90 (R.string.action_rotate_90, R.integer.action_rotate_90) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            //controller.setRotation((controller.getRotation() - 1) % 2);
            if (activity.getRequestedOrientation() == SCREEN_ORIENTATION_LANDSCAPE || activity.getRequestedOrientation() == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                controller.changeOrinatation("PORTRAIT");
            } else {
                controller.changeOrinatation("LANDSCAPE");
            }
        }
    },

    ROTATE_270 (R.string.action_rotate_270, R.integer.action_rotate_270) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            //controller.setRotation((controller.getRotation() + 1) % 2);
            boolean isLevel9 = activity.getOrionContext().getSdkVersion() >= 9;
            if (!isLevel9 || activity.getRequestedOrientation() == SCREEN_ORIENTATION_LANDSCAPE || activity.getRequestedOrientation() == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                ROTATE_90.doAction(controller, activity, parameter);
            } else {
                controller.changeOrinatation("LANDSCAPE_INVERSE");
            }
        }
    },


    INVERSE_CROP (R.string.action_inverse_crops, R.integer.action_inverse_crop) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            TemporaryOptions opts = activity.getOrionContext().getTempOptions();
            opts.inverseCropping = !opts.inverseCropping;

            String title = activity.getResources().getString(R.string.action_inverse_crops) + ":" +(opts.inverseCropping ? "inverted" : "normal");
            Toast.makeText(activity.getApplicationContext(), title, Toast.LENGTH_SHORT).show();
        }
    },

    SWITCH_CROP (R.string.action_switch_long_crop, R.integer.action_switch_long_crop) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            TemporaryOptions opts = activity.getOrionContext().getTempOptions();
            opts.switchCropping = !opts.switchCropping;
            String title = activity.getResources().getString(R.string.action_switch_long_crop) + ":" + (opts.switchCropping ?  "big" : "small");
            Toast.makeText(activity.getApplicationContext(), title, Toast.LENGTH_SHORT).show();
        }
    },

    CROP_LEFT (R.string.action_crop_left, R.integer.action_crop_left) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, true, 0);
        }
    },

    UNCROP_LEFT (R.string.action_uncrop_left, R.integer.action_uncrop_left) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, false, 0);
        }
    },

    CROP_RIGHT (R.string.action_crop_right, R.integer.action_crop_right) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, true, 1);
        }
    },

    UNCROP_RIGHT (R.string.action_uncrop_right, R.integer.action_uncrop_right) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, false, 1);
        }
    },

    CROP_TOP (R.string.action_crop_top, R.integer.action_crop_top) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, true, 2);
        }
    },

    UNCROP_TOP (R.string.action_uncrop_top, R.integer.action_uncrop_top) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, false, 2);
        }
    },

    CROP_BOTTOM (R.string.action_crop_bottom, R.integer.action_crop_bottom) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, true, 3);
        }
    },

    UNCROP_BOTTOM (R.string.action_uncrop_bottom, R.integer.action_uncrop_bottom) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            updateMargin(controller, false, 3);
        }
    };

    private static final HashMap<Integer, Action> actions = new HashMap<>();

    static {
        Action [] values = values();
        for (Action value : values) {
            actions.put(value.code, value);
        }
    }

    private final int name;

    private final int code;


    Action(int nameId, int resId) {
        this.name = nameId;
        this.code = OrionApplication.Companion.getInstance().getResources().getInteger(resId);
    }

    public int getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public static Action getAction(int code) {
        Action result = actions.get(code);
        return result != null ? result : NONE;
    }

    public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {

    }

    protected void updateMargin(Controller controller, boolean isCrop, int index) {
        CropMargins cropMargins = controller.getMargins();
        if (cropMargins.evenCrop && controller.isEvenPage()) {
            if (index == 0 || index == 1) {
                index += 4;
            }
        }

        int[] margins = CropDialogBuilderKt.toDialogMargins(cropMargins);
        OrionApplication context = controller.getActivity().getOrionContext();
        TemporaryOptions tempOpts = context.getTempOptions();
        if (tempOpts.inverseCropping) {
            isCrop = !isCrop;
        }
        int delta = tempOpts.switchCropping ? context.getOptions().getLongCrop() : 1;
        margins[index] += isCrop ? delta : -delta;
        if (margins[index] > OrionViewerActivity.CROP_RESTRICTION_MAX) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MAX;
        }
        if (margins[index] < OrionViewerActivity.CROP_RESTRICTION_MIN) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MIN;
        }

        controller.changeCropMargins(
                CropDialogBuilderKt.toMargins(margins, cropMargins.evenCrop, cropMargins.cropMode)
        );
    }
}
