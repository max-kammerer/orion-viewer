package universe.constellation.orion.viewer;

import android.*;
import android.R;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.*;
import android.widget.PopupWindow;
import android.widget.Toast;

/**
 * User: mike
 * Date: 11.08.12
 * Time: 14:37
 */
public class SelectionAutomata {

    private enum STATE {START, MOVING, END, CANCELED};

    private STATE state = STATE.CANCELED;

    private int startX, startY, width, height;

    private Dialog selectionDialog;

    private SelectionView selectionView;

    private OrionViewerActivity activity;

    public SelectionAutomata(final OrionViewerActivity activity) {
        this.activity = activity;
        //selectionDialog = new Dialog(activity, R.style.Theme_Translucent);
        selectionDialog = new Dialog(activity, (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) > 0 ? R.style.Theme_Translucent_NoTitleBar_Fullscreen : R.style.Theme_Translucent_NoTitleBar);

        selectionDialog.setContentView(universe.constellation.orion.viewer.R.layout.text_selector);

        View view = activity.getLayoutInflater().inflate(universe.constellation.orion.viewer.R.layout.text_selector, null);

        selectionDialog.setContentView(view);
        WindowManager.LayoutParams params = selectionDialog.getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.FILL_PARENT;
        params.height = ViewGroup.LayoutParams.FILL_PARENT;
        selectionDialog.getWindow().setAttributes(params);

        selectionView = (SelectionView) selectionDialog.findViewById(universe.constellation.orion.viewer.R.id.text_selector);

        selectionView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return SelectionAutomata.this.onTouch(event);
            }
        });
    }

    public boolean onTouch(MotionEvent event) {
        int action = event.getAction();

        //System.out.println("aaaction " + action + " " + event.getX() + ", " + event.getY());
        STATE oldState = state;
        boolean result = true;
        switch (state) {
            case START:
                if (action == MotionEvent.ACTION_DOWN) {
                    startX = (int) event.getX();
                    startY = (int) event.getY();
                    width = 0;
                    height = 0;
                    state = STATE.MOVING;
                    selectionView.reset();
                } else {
                    state = STATE.CANCELED;
                }
                break;

            case MOVING:
                int endX = (int) event.getX();
                int endY = (int) event.getY();
                width = endX - startX;
                height = endY - startY;
                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.END;
                } else {
                    selectionView.updateView(Math.min(startX, endX), Math.min(startY, endY), Math.max(startX, endX), Math.max(startY, endY));
                }
                break;

            default: result = false;
        }

        if (oldState != state) {
            switch (state) {
                case CANCELED: selectionDialog.dismiss(); break;

                case END:
                    selectionDialog.dismiss();

                    String text = activity.getController().selectText(getStartX(), getStartY(), getWidth(), getHeight());
                    if (text != null && !"".equals(text)) {
                        new SelectedTextActions(activity).show(text);
                    } else {
                        activity.showFastMessage(universe.constellation.orion.viewer.R.string.warn_no_text_in_selection);
                    }
                    break;
            }
        }
        return result;
    }

    public void startSelection() {
        selectionView.reset();

        selectionDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN & activity.getWindow().getAttributes().flags, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        selectionDialog.show();

        activity.showFastMessage("Select text!");
        state = STATE.START;
    }

    public boolean inSelection() {
        return state == STATE.START || state == STATE.MOVING;
    }

    public boolean isSuccessful() {
        return state == STATE.END;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
