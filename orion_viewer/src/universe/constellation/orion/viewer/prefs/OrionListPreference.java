package universe.constellation.orion.viewer.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.R;

import java.lang.reflect.Field;

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
            return persistValue(value);
        } else {
            return super.persistString(value);
        }
    }

    protected boolean persistValue(String value) {
        LastPageInfo info = ((OrionApplication) getContext().getApplicationContext()).getCurrentBookParameters();
        if (info != null) {
            try {
                Field f = info.getClass().getDeclaredField(getKey());
                Class clazz = f.getType();
                Object resultValue = value;
                if (int.class.equals(clazz)) {
                    resultValue = Integer.valueOf(value);
                }
                f.set(info, resultValue);
                ((OrionApplication)getContext().getApplicationContext()).processBookOptionChange(getKey(), resultValue);
                return true;
            } catch (Exception e) {
                Common.d(e);
            }
        }
        return  false;
    }


    protected int getPersistedInt(int defaultReturnValue) {
        if (isCurrentBookOption) {
            LastPageInfo info = ((OrionApplication) getContext()).getCurrentBookParameters();
            if (info != null) {
                try {
                    Field f = info.getClass().getDeclaredField(getKey());
                    Integer value = (Integer) f.get(info);
                    return value;
                } catch (Exception e) {
                    Common.d(e);
                }
            }
            return defaultReturnValue;
        } else {
            return super.getPersistedInt(defaultReturnValue);
        }
    }


    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (isCurrentBookOption) {
            LastPageInfo info = ((OrionApplication) getContext().getApplicationContext()).getCurrentBookParameters();
            if (info != null) {
                try {
                    Field f = info.getClass().getDeclaredField(getKey());
                    String value = f.get(info).toString();
                    return value;
                } catch (Exception e) {
                    Common.d(e);
                }
            }
            return  defaultReturnValue;
        } else {
            return super.getPersistedString(defaultReturnValue);
        }
    }

}
