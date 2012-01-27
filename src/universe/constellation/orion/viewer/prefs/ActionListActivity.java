package universe.constellation.orion.viewer.prefs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.google.code.orion_viewer.Action;
import pl.polidea.demo.R;

/**
 * User: mike
 * Date: 07.01.12
 * Time: 12:48
 */
public class ActionListActivity extends Activity {

    private boolean populating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getOrionContext().applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.actions_selection);

        RadioGroup group = (RadioGroup) findViewById(R.id.actionsGroup);
        Action[] actions = Action.values();
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            RadioButton button = new RadioButton(this);
            button.setText(getResources().getString(action.getName()));
            button.setTag(R.attr.actionId, action.getCode());
            group.addView(button);
        }

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (!populating) {
                    RadioButton button = (RadioButton) group.findViewById(group.getCheckedRadioButtonId());
                    Integer code = (Integer) button.getTag(R.attr.actionId);
                    Intent result = new Intent();
                    result.putExtra("code", code);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        populating = true;
        int type = getIntent().getIntExtra("type", 0);
        TextView header = (TextView) findViewById(R.id.actions_header);
        header.setText(type == 0 ? R.string.short_click : type == 1 ? R.string.long_click : R.string.binding_click);
        int code = getIntent().getIntExtra("code", 0);
        RadioGroup group = (RadioGroup) findViewById(R.id.actionsGroup);
        int id = group.getChildAt(0).getId();
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton button = (RadioButton) group.getChildAt(i);
            Integer buttone_code = (Integer) button.getTag(R.attr.actionId);
            if (buttone_code == code) {
                id = group.getChildAt(i).getId();
                break;
            }
        }
        group.check(id);
        populating = false;
    }

    public OrionApplication getOrionContext() {
        return (OrionApplication) getApplicationContext();
    }
}
