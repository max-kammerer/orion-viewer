package universe.constellation.orion.viewer.prefs;

import android.preference.Preference;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.LastPageInfo;

import java.lang.reflect.Field;

/**
 * User: mike
 * Date: 07.09.12
 * Time: 12:16
 */
public class OrionPreferenceUtil {

    public static boolean persistValue(Preference pref, String value) {
        OrionApplication appContext = (OrionApplication) pref.getContext().getApplicationContext();
        LastPageInfo info = appContext.getCurrentBookParameters();
        if (info != null) {
            try {
                Field f = info.getClass().getDeclaredField(pref.getKey());
                Class clazz = f.getType();
                Object resultValue = value;
                if (int.class.equals(clazz)) {
                    resultValue = Integer.valueOf(value);
                }
                f.set(info, resultValue);
                ((OrionApplication) appContext.getApplicationContext()).processBookOptionChange(pref.getKey(), resultValue);
                return true;
            } catch (Exception e) {
                Common.d(e);
            }
        }
        return false;
    }

    public static int getPersistedInt(Preference pref, int defaultReturnValue) {
        OrionApplication appContext = (OrionApplication) pref.getContext().getApplicationContext();
        LastPageInfo info = appContext.getCurrentBookParameters();
        if (info != null) {
            try {
                Field f = info.getClass().getDeclaredField(pref.getKey());
                Integer value = (Integer) f.get(info);
                return value;
            } catch (Exception e) {
                Common.d(e);
            }
        }
        return defaultReturnValue;
    }

    public static String getPersistedString(Preference pref, String defaultReturnValue) {
        LastPageInfo info = ((OrionApplication) pref.getContext().getApplicationContext()).getCurrentBookParameters();
        if (info != null) {
            try {
                Field f = info.getClass().getDeclaredField(pref.getKey());
                String value = f.get(info).toString();
                return value;
            } catch (Exception e) {
                Common.d(e);
            }
        }
        return defaultReturnValue;
    }
}
