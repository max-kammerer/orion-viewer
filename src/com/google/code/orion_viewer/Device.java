package com.google.code.orion_viewer;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import android.os.Build;
import android.view.KeyEvent;

/**
 * User: mike
 * Date: 18.10.11
 * Time: 11:13
 */
public interface Device {

    public static class Info {
        public static String MANUFACTURER;
        public static String MODEL;
        public static String DEVICE;
        public static boolean NOOK2;
        public static boolean SONY_PRS_T1;

        static {
            MANUFACTURER = getField("MANUFACTURER");
            MODEL = getField("MODEL");
            DEVICE = getField("DEVICE");
            NOOK2 = MANUFACTURER.toLowerCase().contentEquals("barnesandnoble") && MODEL.contentEquals("NOOK") &&
                    DEVICE.toLowerCase().contentEquals("zoom2");

            SONY_PRS_T1 = MANUFACTURER.toLowerCase().contentEquals("sony") && MODEL.contentEquals("PRS-T1");
        }

        public static String getField(String name) {
            try {
                return (String) Build.class.getField(name).get(null);
            } catch (Exception e) {
                Common.d("Exception on extracting Build property:" + name);
                return "";
            }
        }
    }


    final int NEXT = 1;

    final int PREV = -1;

    final int ESC = 10;

    final int DEFAULT_ACTIVITY = 0;

    final int VIEWER_ACTIVITY = 1;

    void updateTitle(String title);

    boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder operation);

    void onCreate(OrionBaseActivity activity);

    void onPause();

    void onResume();

    void onUserInteraction();

    void updatePageNumber(int current, int max);

    void flushBitmap(int delay);

    public int getLayoutId();

    public int getFileManagerLayoutId();

    public int getHelpLayoutId();

    public String getDefaultDirectory();

    public boolean optionViaDialog();

    public void updateOptions(GlobalOptions options);

}
