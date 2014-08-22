package universe.constellation.orion.viewer;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class universe.constellation.orion.viewer.OrionViewerActivityTest \
 * universe.constellation.orion.viewer.tests/android.test.InstrumentationTestRunner
 */
public class OrionViewerActivityTest extends ActivityInstrumentationTestCase2<OrionViewerActivity> {

    public OrionViewerActivityTest() {
        super("universe.constellation.orion.viewer", OrionViewerActivity.class);
    }

}
