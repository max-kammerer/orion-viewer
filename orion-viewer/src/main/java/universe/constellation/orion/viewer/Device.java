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

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.view.KeyEvent;

import universe.constellation.orion.viewer.device.OnyxUtil;

/**
 * User: mike
 * Date: 18.10.11
 * Time: 11:13
 */
public interface Device {

    int DELAY = 1; //1 min

    int VIEWER_DELAY = 10; //10 min void fullScreen(boolean on, Activity activity);

    class Info {
        public final static String MANUFACTURER = getField("MANUFACTURER");
        public final static String MODEL = getField("MODEL");
        public final static String DEVICE = getField("DEVICE");
        public final static String HARDWARE = getField("HARDWARE");

        public final static boolean NOOK2 = "barnesandnoble".equals(MANUFACTURER.toLowerCase()) && ("NOOK".equals(MODEL) || "BNRV500".equals(MODEL) || "BNRV350".equals(MODEL) || "BNRV300".equals(MODEL) || "unknown".equals(MODEL)) && "zoom2".equals(DEVICE.toLowerCase());

        public final static boolean ONYX_DEVICE = "ONYX".equalsIgnoreCase(MANUFACTURER) && OnyxUtil.isEinkDevice();

        public final static boolean SONY_PRS_T1_T2 = "sony".equals(MANUFACTURER.toLowerCase()) && ("PRS-T1".equals(MODEL) || "PRS-T2".equals(MODEL));

        public final static boolean EDGE = "edge".equals(DEVICE.toLowerCase()) || "edgejr".equals(DEVICE.toLowerCase());

        public final static boolean TEXET_TB_138 = "texet".equalsIgnoreCase(DEVICE) && "rk29sdk".equalsIgnoreCase(MODEL);

        public final static boolean TEXET_TB176FL = "texet".equalsIgnoreCase(MANUFACTURER) && "TB-176FL".equalsIgnoreCase(DEVICE) && "TB-176FL".equalsIgnoreCase(MODEL);

        public final static boolean TEXET_TB576HD = "texet".equalsIgnoreCase(MANUFACTURER) && "TB-576HD".equalsIgnoreCase(DEVICE) && "TB-576HD".equalsIgnoreCase(MODEL);

        public final static boolean RK30SDK = "rk30sdk".equalsIgnoreCase(MODEL) && ("T62D".equalsIgnoreCase(DEVICE) || DEVICE.toLowerCase().contains("onyx") );

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


    int NEXT = 1;

    int PREV = -1;

    int ESC = 10;

    int DEFAULT_ACTIVITY = 0;

    int VIEWER_ACTIVITY = 1;

    void updateTitle(String title);

    boolean onKeyUp(int keyCode, KeyEvent event, OperationHolder operation);

    void onCreate(OrionBaseActivity activity);

    void onNewBook(LastPageInfo info, DocumentWrapper document);

    void onBookClose(LastPageInfo info);

    void onDestroy();
    
    void onPause();

    void onWindowGainFocus();

    void onUserInteraction();

    void flushBitmap();

    int getLayoutId();

    String getDefaultDirectory();

    void onSetContentView();

    Point getDeviceSize();

    boolean isDefaultDarkTheme();

    void fullScreen(boolean on, Activity activity);
}
