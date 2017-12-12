/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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
import android.util.Xml;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static universe.constellation.orion.viewer.LoggerKt.log;

public class LastPageInfo implements Serializable, ShortFileInfo {

    public static final int CURRENT_VERSION = 5;

    public int screenWidth;
    public int screenHeight;

    public int pageNumber;
    public int rotation;

    //application default
    public String screenOrientation = "DEFAULT";

    public int newOffsetX;
    public int newOffsetY;

    public int zoom;

    public int leftMargin = 0;
    public int rightMargin = 0;
    public int topMargin = 0;
    public int bottomMargin = 0;

    public boolean enableEvenCropping = false;
    public int cropMode = 0;
    public int leftEvenMargin = 0;
    public int rightEventMargin = 0;

    public int pageLayout = 0;

    public int contrast = 100;
    public int threshold = 255;

    public transient String fileData;

    public transient long fileSize;

    public transient String simpleFileName;

    public transient String openingFileName;

    public transient int totalPages;

    public String walkOrder = "ABCD";

    public String colorMode = "CM_NORMAL";

    private LastPageInfo() {

    }

    public static LastPageInfo loadBookParameters(OrionBaseActivity activity, String filePath) {
        int idx = filePath.lastIndexOf('/');
        File file = new File(filePath);
        String fileData = filePath.substring(idx + 1) + "." + file.length() + ".xml";
        LastPageInfo lastPageInfo = new LastPageInfo();

        boolean successfull = false;
        try {
            successfull = lastPageInfo.load(activity, fileData);
        } catch (Exception e) {
            log("Error on restore book options", e);
        }

        if (!successfull) {
            //reinit
            lastPageInfo = new LastPageInfo();
            GlobalOptions options = activity.getOrionContext().getOptions();

            lastPageInfo.zoom = options.getDefaultZoom();
            lastPageInfo.contrast = options.getDefaultContrast();
            lastPageInfo.walkOrder = options.getWalkOrder();
            lastPageInfo.pageLayout = options.getPageLayout();
            lastPageInfo.colorMode = options.getColorMode();
        }

        lastPageInfo.fileData = fileData;
        lastPageInfo.openingFileName = filePath;
        lastPageInfo.simpleFileName = filePath.substring(idx + 1);
        lastPageInfo.fileSize = file.length();
        return lastPageInfo;
    }

    public void save(OrionBaseActivity activity) {
        OutputStreamWriter writer = null;
        try {
            XmlSerializer serializer = Xml.newSerializer();
            writer = new OutputStreamWriter(activity.openFileOutput(fileData, Context.MODE_PRIVATE));
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            String nameSpace = "";
            serializer.startTag(nameSpace, "bookParameters");
            serializer.attribute(nameSpace, "version", "" + CURRENT_VERSION);

            Field [] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    int modifiers = field.getModifiers();
                    if ((modifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                        //System.out.println(field.getName());
                        writeValue(serializer, field.getName(), field.get(this).toString());
                    }
                } catch (IllegalAccessException e) {
                    log(e);
                }
            }


//            writeValue(serializer, "screenWidth", screenWidth);
//            writeValue(serializer, "screenHeight", screenHeight);
//
//            writeValue(serializer, "pageNumber", pageNumber);
//            writeValue(serializer, "rotation", rotation);
//            writeValue(serializer, "screenOrientation", screenOrientation);
//
//            writeValue(serializer, "newOffsetX", newOffsetX);
//            writeValue(serializer, "newOffsetY", newOffsetY);
//
//            writeValue(serializer, "zoom", zoom);
//
//            writeValue(serializer, "leftMargin", leftMargin);
//            writeValue(serializer, "rightMargin", rightMargin);
//            writeValue(serializer, "topMargin", topMargin);
//            writeValue(serializer, "bottomMargin", bottomMargin);
//            writeValue(serializer, "leftEvenMargin", leftEvenMargin);
//            writeValue(serializer, "rightEventMargin", rightEventMargin);
//            writeValue(serializer, "enableEvenCropping", enableEvenCropping);
//
//            writeValue(serializer, "navigation", navigation);
//            writeValue(serializer, "pageLayout", pageLayout);


            serializer.endTag(nameSpace, "bookParameters");
            serializer.endDocument();
        } catch (IOException e) {
            log(e);
            activity.showError("Couldn't save book preferences", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log(e);
                }
            }
        }
    }

    public static void writeValue(XmlSerializer serializer, String name, int value) throws IOException {
        writeValue(serializer, name, Integer.toString(value));
    }

    public static void writeValue(XmlSerializer serializer, String name, boolean value) throws IOException {
        writeValue(serializer, name, Boolean.toString(value));
    }

    public static void writeValue(XmlSerializer serializer, String name, String value) throws IOException {
        serializer.startTag("", name);
        serializer.attribute("", "value", value);
        serializer.endTag("", name);
    }

    private boolean load(OrionBaseActivity activity, String filePath) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(activity.openFileInput(filePath));

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            //factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(reader);

            int fileVersion = -1;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = xpp.getName();

                    if ("bookParameters".equals(name)) {
                        fileVersion = Integer.valueOf(xpp.getAttributeValue("", "version"));
                    } else {
                        try {
                            String rawValue = xpp.getAttributeValue("", "value");
                            Field f = getClass().getField(name);
                            Object value;
                            Class type = f.getType();
                            if (type.equals(int.class)) {
                                value = Integer.valueOf(rawValue);
                                value = upgrade(fileVersion, name, (Integer) value);
                            } else if (type.equals(boolean.class)) {
                                value = Boolean.valueOf(rawValue);
                            } else if (type.equals(String.class)) {
                                value = rawValue;
                            } else {
                                log("Error on deserializing field " + name + " = " + rawValue);
                                continue;
                            }
                            getClass().getField(name).set(this, value);
                        } catch (IllegalAccessException e) {
                            log(e);
                        } catch (NoSuchFieldException e) {
                            //skip
                            log(e);
                        } catch (NumberFormatException e) {
                            log(e);
                        }
                    }
                }
                eventType = xpp.next();
            }
            return true;
        } catch (FileNotFoundException e) {
            //do nothing
        } catch (XmlPullParserException e) {
            activity.showError("Couldn't parse book parameters", e);
        } catch (IOException e) {
            activity.showError("Couldn't parse book parameters", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        return false;
    }

    public Integer upgrade(int fromVersion, String name, Integer value) {
        int localVersion = fromVersion;
        if (localVersion < 2) {
            if ("zoom".equals(name)) {
                System.out.println("Property " + name + " upgraded");
                localVersion = 2;
                value = 0;
            }
        }

        if (localVersion < 3) {
            if ("rotation".equals(name)) {
                System.out.println("Property " + name + " upgraded");
                localVersion = 3;
                value = 0;
            }
        }

        if (localVersion < 4) {
            if ("navigation".equals(name)) {
                System.out.println("Property " + name + " upgraded");
                localVersion = 4;
                if (value == 1) {
                    walkOrder = "ACBD";
                }
            }
        }

        if (localVersion < 5) {
            if ("contrast".equals(name)) {
                System.out.println("Property " + name + " upgraded");
                localVersion = 5;
                value = 100;
            }
        }

        return value;
    }

    @Override
    public int getCurrentPage() {
        return pageNumber;
    }

    @NotNull
    @Override
    public String getFileName() {
        return openingFileName;
    }

    @NotNull
    @Override
    public String getSimpleFileName() {
        return simpleFileName;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }
}
