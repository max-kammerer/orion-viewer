package universe.constellation.orion.viewer.selection;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

public class SelectedTextActions {

    private final PopupWindow popup;

    private String text;

    private final Dialog originalDialog;

    public SelectedTextActions(final OrionViewerActivity activity, final Dialog originalDialog) {
        this.originalDialog = originalDialog;
        popup = new PopupWindow(activity);
        popup.setFocusable(true);
        popup.setTouchable(true);
        popup.setOutsideTouchable(true);
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        View view = activity.getLayoutInflater().inflate(activity.isNewUI() ? R.layout.text_actions_new : R.layout.text_actions, null);

        popup.setContentView(view);

        ImageButton copy_to_Clipboard = view.findViewById(R.id.stext_copy_to_clipboard);
        copy_to_Clipboard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
                activity.showFastMessage("Copied to clipboard");

            }
        });

        ImageButton add_bookmark = view.findViewById(R.id.stext_add_bookmark);
        add_bookmark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.ADD_BOOKMARK.doAction(activity.getController(), activity, text);
            }
        });

        ImageButton open_dictionary = view.findViewById(R.id.stext_open_dictionary);
        open_dictionary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.DICTIONARY.doAction(activity.getController(), activity, text);
            }
        });

        ImageButton external_actions = view.findViewById(R.id.stext_send_text);
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

    public void show(String text) {
        this.text = text;
        popup.showAtLocation(originalDialog.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

}
