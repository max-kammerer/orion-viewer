package universe.constellation.orion.viewer;

import android.view.KeyEvent;

/**
 * User: mike
 * Date: 14.08.13
 * Time: 9:41
 */
public class SafeApi {

    public static final void doTrackEvent(KeyEvent event) {
        event.startTracking();
    }

    public static final boolean isCanceled(KeyEvent event) {
        return event.isCanceled();
    }

}
