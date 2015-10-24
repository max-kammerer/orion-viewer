package universe.constellation.orion.viewer.dialog;

import android.app.Dialog;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.WindowManager;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.view.OrionDrawScene;
import universe.constellation.orion.viewer.OrionViewerActivity;

/**
 * User: mike
 * Date: 12.11.13
 * Time: 20:40
 */
public class DialogOverView {

    protected OrionViewerActivity activity;
    private int layoutId;

    protected final Dialog dialog;

    public DialogOverView(OrionViewerActivity activity, int layoutId, int style) {
        this.activity = activity;
        this.layoutId = layoutId;

        dialog = new Dialog(activity, style);
        dialog.setContentView(layoutId);

    }

    protected void initDialogSize() {
        OrionDrawScene orionDrawScene = activity.getView();
        Rect rect = orionDrawScene.getViewCoords();
        int width = rect.width();
        int height = rect.height();
        Common.d("Dialog dim: " + width + "x" + height);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = width;
        params.height = height;
        dialog.getWindow().setAttributes(params);
    }
}
