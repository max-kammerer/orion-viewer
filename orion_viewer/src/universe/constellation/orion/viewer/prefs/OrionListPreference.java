package universe.constellation.orion.viewer.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 25.08.12
 * Time: 13:26
 */
public class OrionListPreference extends ListPreference {

    private boolean isCurrentBookOption;

    public OrionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OrionListPreference(Context context) {
        super(context);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference);
        isCurrentBookOption = a.getBoolean(R.styleable.universe_constellation_orion_viewer_prefs_OrionListPreference_isBook, false);
        a.recycle();
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (isCurrentBookOption && !restoreValue) {
            //for android 1.5
            restoreValue = true;
        }
        super.onSetInitialValue(restoreValue, defaultValue);
    }

    @Override
    protected boolean persistString(String value) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.persistValue(this, value);
        } else {
            return super.persistString(value);
        }
    }

    protected String getPersistedString(String defaultReturnValue) {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedString(this, defaultReturnValue);
        } else {
            return super.getPersistedString(defaultReturnValue);
        }
    }

}
