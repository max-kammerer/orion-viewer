package universe.constellation.orion.viewer.dialog;

import android.graphics.Rect;
import android.view.WindowManager;
import org.holoeverywhere.app.Dialog;
import universe.constellation.orion.viewer.OrionView;
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
        OrionView orionView = activity.getView();
        Rect rect = orionView.getViewCoords();
        int[] coords = new int[]{rect.left, rect.top};
        orionView.getLocationOnScreen(coords);
        int left = coords[0];
        int top = coords[1];
        int width = rect.width();
        int height = rect.height();
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = width;
        params.height = height;
        params.x = left;
        params.y = top;
        dialog.getWindow().setAttributes(params);
    }
}
