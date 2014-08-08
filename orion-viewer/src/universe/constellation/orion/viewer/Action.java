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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.widget.Toast;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeViewList;
import universe.constellation.orion.viewer.outline.OutlineAdapter;
import universe.constellation.orion.viewer.outline.OutlineItem;
import universe.constellation.orion.viewer.prefs.*;

import java.util.HashMap;

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
            activity.openOptionsMenu();
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
			Common.d("In Show Outline!");
			OutlineItem[] outline = controller.getOutline();

			if (outline != null && outline.length != 0) {
                final Dialog dialog = activity.createThemedDialog();
                dialog.setTitle(R.string.menu_outline_text);
                dialog.setContentView(R.layout.outline);

                InMemoryTreeStateManager<Integer> manager = new InMemoryTreeStateManager<Integer>();
                manager.setVisibleByDefault(false);
                OutlineAdapter.initializeTreeManager(manager, outline);
                TreeViewList tocTree = (TreeViewList) dialog.findViewById(R.id.mainTreeView);
                tocTree.setDivider(activity.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));

                tocTree.setAdapter(new OutlineAdapter(controller, activity, dialog, manager, outline));
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
//                activity.getOrionContext().getTempOptions().outline = outline;
//                Intent intent = new Intent(activity, OutlineActivity.class);
//                activity.startActivityForResult(intent, OrionViewerActivity.OPEN_BOOKMARK_ACTIVITY_RESULT);
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
            activity.textSelectionMode();
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

    DAY_NIGHT (R.string.action_day_night_mode, R.integer.action_day_night_mode) {
        public void doAction(Controller controller, OrionViewerActivity activity, Object parameter) {
            activity.changeDayNightMode();
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
                parameter = parameter == null ? "" : parameter;
            } else if ("LINGVO".equals(dict)) {
                action = "com.abbyy.mobile.lingvo.intent.action.TRANSLATE";
                intent.setPackage("com.abbyy.mobile.lingvo.market");
                queryText = "com.abbyy.mobile.lingvo.intent.extra.TEXT";
                parameter = parameter == null ? "" : parameter;
            }

            if (action != null) {
                intent.setAction(action);
                if (parameter != null) {
                    intent.putExtra(queryText, (String) parameter);
                }

                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Common.d(ex);
                    String string = activity.getString(R.string.warn_msg_no_dictionary);
                    activity.showWarning(string + ": " + dict + ": " + ex.getMessage());
                }
            }
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
            if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.getRequestedOrientation() == 8) {
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
            if (!isLevel9 || activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.getRequestedOrientation() == 8) {
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

    private static final HashMap<Integer, Action> actions = new HashMap<Integer, Action>();

    static {
        Action [] values = values();
        for (int i = 0; i < values.length; i++) {
            Action value = values[i];
            actions.put(value.code, value);
        }
    }

    private final int name;

    private final int code;


    Action(int nameId, int resId) {
        this.name = nameId;
        this.code = OrionApplication.instance.getResources().getInteger(resId);
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
        if (controller.isEvenCropEnabled() && controller.isEvenPage()) {
            if (index == 0 || index == 1) {
                index += 4;
            }
        }
        int [] margins = new int[6];
        controller.getMargins(margins);
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
        controller.changeMargins(margins);
    }
}
