package universe.constellation.orion.viewer.dialog;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.prefs.OrionTapActivity;

/**
 * User: mike
 * Date: 12.11.13
 * Time: 20:47
 */
public class TapHelpDialog extends DialogOverView {

    public TapHelpDialog(OrionViewerActivity activity) {
        super(activity, R.layout.tap, android.R.style.Theme_Translucent);
        dialog.setTitle(R.string.tap_zones_header);

        TableLayout table = dialog.findViewById(R.id.tap_table);
        table.setBackgroundColor(0);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View layout = row.getChildAt(j);

                TextView shortText = layout.findViewById(R.id.shortClick);
                TextView longText = layout.findViewById(R.id.longClick);
                longText.setVisibility(View.GONE);

                int shortCode = prefs.getInt(OrionTapActivity.getKey(i, j, false), -1);
                if (shortCode == -1) {
                    shortCode = OrionTapActivity.getDefaultAction(i, j, false);
                }
                Action saction = Action.getAction(shortCode);
                //ffcc66
                layout.setBackgroundColor(saction == Action.NEXT ? 0xDDddaa44 : (saction == Action.PREV ? 0xDDeebb55 : 0xDDffcc66));

                shortText.setText(activity.getResources().getString(saction.getName()));

                shortText.setTextSize(20);


                //if (!(i == 1 && j == 1)) {
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(i == 1 && j == 1 ? RelativeLayout.ALIGN_TOP : RelativeLayout.CENTER_IN_PARENT);
                    shortText.setLayoutParams(layoutParams);
                //}
            }
        }

        ImageView view = dialog.findViewById(R.id.tap_help_close);
        view.setVisibility(View.VISIBLE);
        view.setClickable(true);
        view.setOnClickListener(v -> {
            dialog.dismiss();
            activity.getGlobalOptions().saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false);
        });
    }

    public void showDialog() {
        initDialogSize();
        dialog.show();
    }

}
