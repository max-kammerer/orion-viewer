package universe.constellation.orion.viewer.dialog;

import static universe.constellation.orion.viewer.LoggerKt.log;

import android.app.Dialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import universe.constellation.orion.viewer.OrionScene;
import universe.constellation.orion.viewer.OrionViewerActivity;

public class DialogOverView {

    protected final OrionViewerActivity activity;

    public final Dialog dialog;

    public DialogOverView(OrionViewerActivity activity, int layoutId, int style) {
        this.activity = activity;

        dialog = new Dialog(activity, style);
        dialog.setContentView(layoutId);
    }

    protected void initDialogSize() {
        OrionScene view = activity.getView();
        int width = view.getSceneWidth();
        int height = view.getSceneHeight();
        log("Dialog dim: " + width + "x" + height);
        Window window = dialog.getWindow();
        if (window == null) return;
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.TOP;
        params.width = width;
        params.height = height;
        params.y = view.getSceneYLocationOnScreen();
        window.setAttributes(params);
    }
}
