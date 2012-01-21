package com.google.code.orion_viewer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import universe.constellation.orion.viewer.OrionFileManagerActivity;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

import java.util.HashMap;

/**
 * User: mike
 * Date: 06.01.12
 * Time: 18:28
 */
public enum Action {

    NONE (R.string.action_none, 0),

    MENU (R.string.action_menu, 1) ,

    NEXT (R.string.action_next_page, 2),

    PREV (R.string.action_prev_page, 3),

    NEXT10 (R.string.action_next_10, R.integer.action_next_10) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity) {
            int page = controller.getCurrentPage() + 10;

            if (page > controller.getPageCount() - 1) {
                page = controller.getPageCount() - 1;
            }
            controller.drawPage(page);
        }
    },


    PREV10 (R.string.action_prev_10, R.integer.action_prev_10) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity) {
            int page = controller.getCurrentPage() - 10;

            if (page < 0) {
                page = 0;
            }
            controller.drawPage(page);
        }
    },

    ZOOM (R.string.action_zoom_page, 4),

    CROP (R.string.action_crop_page, 5),

    OPTIONS (R.string.action_options_page, 6),

    GOTO (R.string.action_goto_page, 7),

    ROTATION (R.string.action_rotation_page, 8),

    ROTATE_90 (R.string.action_rotate_90, R.integer.action_rotate_90) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity) {
            controller.setRotation((controller.getRotation() - 1) % 2);
        }
    },

    ROTATE_270 (R.string.action_rotate_270, R.integer.action_rotate_270) {
        @Override
        public void doAction(Controller controller, OrionViewerActivity activity) {
            controller.setRotation((controller.getRotation() + 1) % 2);
        }
    },

    DICTIONARY (R.string.action_dictionary, R.integer.action_dictionary) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            String dict = activity.getGlobalOptions().getDictionary();
            String action = null;
            String additional = null;
            if ("FORA".equals(dict)) {
                action = "com.ngc.fora.action.LOOKUP";
                additional = "HEADWORD";
            } else if ("COLORDICT".equals(dict)) {
                action = "colordict.intent.action.SEARCH";
            } else if ("AARD".equals(dict)) {
                action = Intent.ACTION_MAIN;
                additional = "aarddict.android.LookupActivity";
            }
            if (action != null) {
                Intent intent = new Intent(action);
                if (additional != null) {
                    intent.setClassName("aarddict.android", additional);
                }
                //intent.putExtra(additional, "test");
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Common.d(ex);
                }
            }
        }
    },

    OPEN_BOOK (R.string.action_open, R.integer.action_open_book) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            Intent intent = new Intent(activity, OrionFileManagerActivity.class);
            activity.startActivity(intent);
        }
    },

    CROP_LEFT (R.string.action_crop_left, R.integer.action_crop_left) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, +1, 0);
        }
    },

    UNCROP_LEFT (R.string.action_uncrop_left, R.integer.action_uncrop_left) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, -1, 0);
        }
    },

    CROP_RIGHT (R.string.action_crop_right, R.integer.action_crop_right) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, +1, 1);
        }
    },

    UNCROP_RIGHT (R.string.action_uncrop_right, R.integer.action_uncrop_right) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, -1, 1);
        }
    },

    CROP_TOP (R.string.action_crop_top, R.integer.action_crop_top) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, +1, 2);
        }
    },

    UNCROP_TOP (R.string.action_uncrop_top, R.integer.action_uncrop_top) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, -1, 2);
        }
    },

    CROP_BOTTOM (R.string.action_crop_bottom, R.integer.action_crop_bottom) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, +1, 3);
        }
    },

    UNCROP_BOTTOM (R.string.action_uncrop_bottom, R.integer.action_uncrop_bottom) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            updateMargin(controller, -1, 3);
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

    Action(int nameId, int code) {
        this.name = nameId;
        this.code = code;
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

    public void doAction(Controller controller, OrionViewerActivity activity) {

    }

    protected void updateMargin(Controller controller, int delta, int index) {
        int [] margins = new int[4];
        controller.getMargins(margins);
        margins[index] += delta;
        if (margins[index] > OrionViewerActivity.CROP_RESTRICTION_MAX) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MAX;
        }
        if (margins[index] < OrionViewerActivity.CROP_RESTRICTION_MIN) {
            margins[index] = OrionViewerActivity.CROP_RESTRICTION_MIN;
        }
        controller.changeMargins(margins);
    }
}
