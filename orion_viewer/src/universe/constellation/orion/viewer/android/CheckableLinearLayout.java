package universe.constellation.orion.viewer.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

/**
 * User: mike
 * Date: 06.09.12
 * Time: 14:59
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {

    private Checkable checkbox;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
	}

    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
    	// find checked text view
		int childCount = getChildCount();
		for (int i = 0; i < childCount; ++i) {
			View v = getChildAt(i);
			if (v instanceof Checkable) {
				checkbox = (Checkable)v;
			}
		}
    }

    public boolean isChecked() {
        return checkbox != null ? checkbox.isChecked() : false;
    }

    public void setChecked(boolean checked) {
    	if (checkbox != null) {
    		checkbox.setChecked(checked);
    	}
    }

    public void toggle() {
    	if (checkbox != null) {
    		checkbox.toggle();
    	}
    }
}