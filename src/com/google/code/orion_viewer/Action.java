package com.google.code.orion_viewer;

import android.content.ActivityNotFoundException;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Dictionary;
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

    ZOOM (R.string.action_zoom_page, 4),

    CROP (R.string.action_crop_page, 5),

    OPTIONS (R.string.action_options_page, 6),

    GOTO (R.string.action_goto_page, 7),

    ROTATION (R.string.action_rotation_page, 8),

    DICTIONARY (R.string.action_dictionary, 9) {
        public void doAction(Controller controller, OrionViewerActivity activity) {
            String dict = activity.getGlobalOptions().getDictionary();
            String action = null;
            String additional = null;
            if ("FORA".equals(dict)) {
                action = "com.ngc.fora.action.LOOKUP";
                additional = "HEADWORD";
            } else if ("COLORDICT".equals(dict)) {
                action = "colordict.intent.action.SEARCH";
            }
            if (action != null) {
                Intent intent = new Intent(action);
                //intent.putExtra(additional, "test");
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Common.d(ex);
                }
            }
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
}
