package universe.constellation.orion.viewer.selection;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionBaseActivityKt;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

public class SelectedTextActions {

    private final PopupWindow popup;

    private final int height;

    private String text;

    private final Dialog originalDialog;

    public SelectedTextActions(final OrionViewerActivity activity, final Dialog originalDialog) {
        height = activity.getView().getSceneHeight();
        this.originalDialog = originalDialog;
        popup = new PopupWindow(activity);
        //popup.setFocusable(true);
        popup.setTouchable(true);
        //popup.setOutsideTouchable(true);
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        View view = activity.getLayoutInflater().inflate(R.layout.text_actions_new, null);

        popup.setContentView(view);

        ImageView copy_to_Clipboard = view.findViewById(R.id.stext_copy_to_clipboard);
        copy_to_Clipboard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                activity.showFastMessage("Copied to clipboard");

            }
        });

        ImageView add_bookmark = view.findViewById(R.id.stext_add_bookmark);
        add_bookmark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.ADD_BOOKMARK.doAction(activity.getController(), activity, text);
            }
        });

        ImageView open_dictionary = view.findViewById(R.id.stext_open_dictionary);
        open_dictionary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.DICTIONARY.doAction(activity.getController(), activity, text);
            }
        });

        ImageView external_actions = view.findViewById(R.id.stext_send_text);
        external_actions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                activity.startActivity(Intent.createChooser(intent, null));
            }
        });

        popup.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    popup.dismiss();
                    return true;
                }
                return false;
            }
        });

        popup.setOnDismissListener(originalDialog::dismiss);
    }

    public void show(String text, RectF selectionRect) {
        float x = selectionRect.left, y = 0;
        System.out.println(selectionRect);
        if (selectionRect.bottom <= height * 4 /5) {
            y = (int) (selectionRect.bottom + OrionBaseActivityKt.dpToPixels(originalDialog.getContext(), 5));
        } else if (selectionRect.top >= height / 5) {
            System.out.println("1 " + popup.getHeight());
            y = (int) (selectionRect.top - OrionBaseActivityKt.dpToPixels(originalDialog.getContext(), 60));
        } else {
            y = selectionRect.centerY();
        }
        this.text = text;
        View decorView = originalDialog.getWindow().getDecorView();
        popup.showAsDropDown(decorView, (int) x, (int) (-decorView.getHeight() + y));
    }

    public void dismissOnlyDialog() {
        popup.setOnDismissListener(null);
        popup.dismiss();

    }

}
