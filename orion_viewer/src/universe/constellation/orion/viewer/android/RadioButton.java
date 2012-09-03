package universe.constellation.orion.viewer.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 03.09.12
 * Time: 21:44
 */
public class RadioButton extends android.widget.RadioButton {

    private String walkOrder;

    public RadioButton(Context context) {
        super(context);
    }

    public RadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RadioButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_android_RadioButton);
            walkOrder = a.getString(R.styleable.universe_constellation_orion_viewer_android_RadioButton_walkOrder);
            System.out.println(getClass() + " " + walkOrder);
            a.recycle();
        }
    }

    public String getWalkOrder() {
        return walkOrder;
    }
}
