/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;
import universe.constellation.orion.viewer.prefs.OrionApplication;

/**
 * User: mike
 * Date: 18.10.11
 * Time: 11:13
 */
public interface Device {

    final static int DELAY = 1; //1 min

    final static int VIEWER_DELAY = 10; //10 min

    public static class Info {
        public final static String MANUFACTURER = getField("MANUFACTURER");
        public final static String MODEL = getField("MODEL");
        public final static String DEVICE = getField("DEVICE");

        public final static boolean NOOK2 = "barnesandnoble".equals(MANUFACTURER.toLowerCase()) && ("NOOK".equals(MODEL) || "BNRV350".equals(MODEL) || "BNRV300".equals(MODEL) || "unknown".equals(MODEL)) && "zoom2".equals(DEVICE.toLowerCase());

        public final static boolean NOOK_120 = NOOK2 && ("1.2.0".equals(getVersion()) || "1.2.1".equals(getVersion()));
        
        public final static boolean SONY_PRS_T1_T2 = "sony".equals(MANUFACTURER.toLowerCase()) && ("PRS-T1".equals(MODEL) || "PRS-T2".equals(MODEL));

        public final static boolean EDGE = "edge".equals(DEVICE.toLowerCase()) || "edgejr".equals(DEVICE.toLowerCase());

        public final static boolean TEXET_TB_138 = "texet".equalsIgnoreCase(DEVICE) && "rk29sdk".equalsIgnoreCase(MODEL);

		public static String getField(String name) {
            try {
                return (String) Build.class.getField(name).get(null);
            } catch (Exception e) {
                Common.d("Exception on extracting Build property:" + name);
                return "";
            }
        }
		
		public static String getVersion() {
			return Build.VERSION.INCREMENTAL;
		}
		
    }


    final int NEXT = 1;

    final int PREV = -1;

    final int ESC = 10;

    final int DEFAULT_ACTIVITY = 0;

    final int VIEWER_ACTIVITY = 1;

    void updateTitle(String title);

    boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder operation);

    void onCreate(OrionBaseActivity activity);

    void onDestroy();
    
    void onPause();

    void onWindowGainFocus();

    void onUserInteraction();

    void updatePageNumber(int current, int max);

    void flushBitmap(int delay);

    int getLayoutId();

    int getFileManagerLayoutId();

    String getDefaultDirectory();

    void onSetContentView();

    Point getDeviceSize();

}
