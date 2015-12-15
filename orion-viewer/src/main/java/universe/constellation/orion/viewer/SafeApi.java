package universe.constellation.orion.viewer;

import android.view.KeyEvent;

/**
 * User: mike
 * Date: 14.08.13
 * Time: 9:41
 */
public class SafeApi {

    public static void doTrackEvent(KeyEvent event) {
        event.startTracking();
    }

    public static boolean isCanceled(KeyEvent event) {
        return event.isCanceled();
    }

}
