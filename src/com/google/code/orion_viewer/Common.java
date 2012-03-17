package com.google.code.orion_viewer;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
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

import android.util.Log;
import com.google.code.orion_viewer.device.AlexDevice;
import com.google.code.orion_viewer.device.AndroidDevice;
import com.google.code.orion_viewer.device.EdgeDevice;
import com.google.code.orion_viewer.device.NookDevice;

import java.io.*;

/**
 * User: mike
 * Date: 13.11.11
 * Time: 11:54
 */
public class Common {

    public static Device createDevice() {
         return new AndroidDevice();
    }

    public static final String LAST_OPENED_DIRECTORY = "LAST_OPENED_DIR";

    public static final String LOGTAG = "Orion_Viewer";

    public static PrintWriter writer;

    public static void startLogger(String file) {
//        if (writer != null) {
//            stopLogger();
//        }
//        try {
//            writer = new PrintWriter(new FileWriter(file));
//        } catch (Exception e) {
//            e.printStackTrace();
//            if (writer != null) {
//                writer.close();
//            }
//        }
    }

    public static void stopLogger() {
        if(writer != null) {
            writer.close();
            writer = null;
        }
    }

    public static void d(String message) {
        Log.d(LOGTAG, message);
        if (writer != null) {
            writer.write(message);
            writer.write("\n");
        }
    }

    public static void d(Exception e) {
        e.printStackTrace();
        if (writer != null) {
            e.printStackTrace(writer);
            writer.write("\n");
        }
    }

    public static void i(String message) {
        Common.d(message);
    }

}
