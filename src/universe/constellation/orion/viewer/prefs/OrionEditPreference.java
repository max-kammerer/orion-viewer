package universe.constellation.orion.viewer.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;
import com.google.code.orion_viewer.Action;
import com.google.code.orion_viewer.Common;
import universe.constellation.orion.viewer.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: mike
 * Date: 25.01.12
 * Time: 12:41
 */
public class OrionEditPreference extends EditTextPreference  implements Preference.OnPreferenceChangeListener {

    private Integer minValue;
    private Integer maxValue;

    private Boolean notEmpty;

    private String pattern;

    private CharSequence originalSummary;

    public OrionEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initValidator(attrs);
    }

    public OrionEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initValidator(attrs);
    }

    public OrionEditPreference(Context context) {
        super(context);
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
        System.out.println(pattern);
        if (Pattern.compile(pattern).matcher((String) newValue).matches()){
            return true;
        } else {
            //Toast.makeText(getContext(), "asdadad", Toast.LENGTH_SHORT);
            return false;
        }
    }

    public void initValidator(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference);

        pattern = a.getString(R.styleable.universe_constellation_orion_viewer_prefs_OrionEditPreference_pattern);
        a.recycle();
        if (pattern != null) {
            setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        if (originalSummary != null) {
            originalSummary = summary;
        }
        super.setSummary(summary);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
