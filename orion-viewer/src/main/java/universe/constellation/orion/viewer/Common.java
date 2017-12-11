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

import android.os.Build;
import android.util.Log;

import java.io.FileWriter;
import java.io.PrintWriter;

import universe.constellation.orion.viewer.device.Device;

/**
 * User: mike
 * Date: 13.11.11
 * Time: 11:54
 */
public class Common {

    public static final String LAST_OPENED_DIRECTORY = "LAST_OPENED_DIR";

    public static final String LOGTAG = "OrionViewer";

    public static PrintWriter writer;

    public static void startLogger(String file) {
        if (writer != null) {
            stopLogger();
        }
        try {
            writer = new PrintWriter(new FileWriter(file));
        } catch (Exception e) {
            e.printStackTrace();
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static void stopLogger() {
        if(writer != null) {
            writer.close();
            writer = null;
        }
    }

    public static void d(String message, Exception e) {
        d(message);
        d(e);
    }

    public static void d(String message) {
        Log.d(LOGTAG, message);
        if (writer != null) {
            writer.write(message);
            writer.write("\n");
        }
    }

    public static void d(Exception e) {
        Log.d(LOGTAG, e.getMessage(), e);
        if (writer != null) {
            e.printStackTrace(writer);
            writer.write("\n");
        }
    }

    public static void logOrionAndDeviceInfo() {
        Common.d("Orion Viewer " + BuildConfig.VERSION_NAME);
        Common.d("Device: " + Device.Info.DEVICE);
        Common.d("Model: " + Device.Info.MODEL);
        Common.d("Manufacturer:  " + Device.Info.MANUFACTURER);
        Common.d("Android version :  " + Build.VERSION.CODENAME + " " + Build.VERSION.RELEASE);
    }

}
