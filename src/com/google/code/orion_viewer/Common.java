package com.google.code.orion_viewer;

import android.util.Log;

import java.io.*;

/**
 * User: mike
 * Date: 13.11.11
 * Time: 11:54
 */
public class Common {

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
