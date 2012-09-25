package universe.constellation.orion.viewer.selection;

import android.app.Dialog;
import android.content.Intent;
import android.view.*;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 12.08.12
 * Time: 12:27
 */
public class SelectedTextActions {

    private PopupWindow popup;

    private String text;

    private OrionViewerActivity activity;

    public SelectedTextActions(final OrionViewerActivity activity) {
        this.activity = activity;
        popup = new PopupWindow(activity);
        popup.setFocusable(true);
        popup.setTouchable(true);
        popup.setOutsideTouchable(true);
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        View view = activity.getLayoutInflater().inflate(R.layout.text_actions, null);

        popup.setContentView(view);

        ImageButton add_bookmark = (ImageButton) view.findViewById(R.id.stext_add_bookmark);
        add_bookmark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.ADD_BOOKMARK.doAction(activity.getController(), activity, text);
            }
        });

        ImageButton open_dictionary = (ImageButton) view.findViewById(R.id.stext_open_dictionary);
        open_dictionary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                Action.DICTIONARY.doAction(activity.getController(), activity, text);
            }
        });

        ImageButton external_actions = (ImageButton) view.findViewById(R.id.stext_send_text);
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
                if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
                    popup.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    public void show(String text) {
        this.text = text;
        popup.showAtLocation(activity.getView(), Gravity.CENTER, 0, 0);
    }

}
