package universe.constellation.orion.viewer.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import universe.constellation.orion.viewer.LastPageInfo;
import universe.constellation.orion.viewer.OrionBaseActivity;

/**
 * User: mike
 * Date: 1/12/14
 * Time: 1:07 PM
 */
public class TexetTB176FLDevice extends TexetDevice {

    private OrionBaseActivity activity;

    @Override
    public void onCreate(OrionBaseActivity activity) {
        super.onCreate(activity);
        this.activity = activity;
    }

    @Override
    public boolean isDefaultDarkTheme() {
        return false;
    }

    @Override
    public boolean isLightingSupported() {
        return true;
    }

    @Override
    public int doLighting(int delta) throws Exception {
        int brightness = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);

        int newBrightness = brightness + delta / 5;
        if (newBrightness < 0) {
            newBrightness = 0;
        }
        if (newBrightness > 255) {
            newBrightness = 255;
        }

        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

        Method setBacklight = pm.getClass().getMethod("setBacklight", Integer.TYPE);
        setBacklight.invoke(pm, brightness);
        Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);

        return newBrightness;
    }
}
