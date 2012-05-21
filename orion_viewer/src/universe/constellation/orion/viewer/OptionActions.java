package universe.constellation.orion.viewer;

import android.view.WindowManager;

import java.util.HashMap;

/**
 * User: mike
 * Date: 20.05.12
 * Time: 16:41
 */
public enum OptionActions {

    NONE("NONE"),

    FULL_SCREEN("FULL_SCREEN") {
        public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {
            activity.getWindow().setFlags(newValue ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    },


    SCREEN_OVERLAPPING_HORIZONTAL("SCREEN_OVERLAPPING_HORIZONTAL") {
        public void doAction(OrionViewerActivity activity, int hor, int ver) {
            activity.getController().changeOverlap(hor, ver);
        }
    },

    SCREEN_OVERLAPPING_VERTICAL("SCREEN_OVERLAPPING_VERTICAL") {
        public void doAction(OrionViewerActivity activity, int hor, int ver) {
            activity.getController().changeOverlap(hor, ver);
        }
    },




    SET_CONSTRAST("contrast") {
        public void doAction(OrionViewerActivity activity, int oldValue, int newValue) {
            Controller controller = activity.getController();
            if (controller != null) {
                controller.changeContrast(newValue);
            }
        }
    };


    private static final HashMap<String, OptionActions> actions = new HashMap<String, OptionActions>();

    static {
        OptionActions[] values = values();
        for (int i = 0; i < values.length; i++) {
            OptionActions value = values[i];
            actions.put(value.getKey(), value);
        }
    }

    public static OptionActions getAction(String key) {
        OptionActions result = actions.get(key);
        return result != null ? result : NONE;
    }

    private String key;

    OptionActions(String key) {
        this.key = key;
    }

    public void doAction(OrionViewerActivity activity, int oldValue, int newValue) {

    }

    public void doAction(OrionViewerActivity activity, boolean oldValue, boolean newValue) {

    }

    public void doAction(OrionViewerActivity activity, String oldValue, String newValue) {

        }




    public String getKey() {
        return key;
    }
}
