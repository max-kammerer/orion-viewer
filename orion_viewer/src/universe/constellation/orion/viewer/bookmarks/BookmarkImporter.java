/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
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

package universe.constellation.orion.viewer.bookmarks;

import android.database.Cursor;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.OrionBaseActivity;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: mike
 * Date: 30.09.12
 * Time: 10:25
 */
public class BookmarkImporter {

    private BookmarkAccessor dataBase;

    private String inputName;

    private OrionBaseActivity activity;

    public BookmarkImporter(OrionBaseActivity activity, BookmarkAccessor dataBase, String inputName) {
        this.activity = activity;
        this.dataBase = dataBase;
        this.inputName = inputName;
    }


    public boolean importToBook(File file, long size) throws IOException {
        Cursor c = null;
        FileWriter writer = null;
        try {
            long bookId = dataBase.insertOrUpdate(file.getName(), size);

            if (bookId == -1) {
                activity.showWarning("Couldn't insert new book to database!");
                return false;
            }

            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(new FileInputStream(file));

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                //factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(reader);

                int fileVersion = -1;
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String name = xpp.getName();

                        if ("bookmarks".equals(name)) {
                            fileVersion = Integer.valueOf(xpp.getAttributeValue("", "version"));
                        } else {
                            try {
                                String rawValue = xpp.getAttributeValue("", "value");
                                Field f = getClass().getField(name);
                                Object value  = null;
                                Class type = f.getType();
                                if (type.equals(int.class)) {
                                    value = Integer.valueOf(rawValue);
                                } else if (type.equals(boolean.class)) {
                                    value = Boolean.valueOf(rawValue);
                                } else if (type.equals(String.class)) {
                                    value = rawValue;
                                } else {
                                    Common.d("Error on deserializing field " + name + " = " + rawValue);
                                    continue;
                                }
                                getClass().getField(name).set(this, value);
                            } catch (IllegalAccessException e) {
                                Common.d(e);
                            } catch (NoSuchFieldException e) {
                                //skip
                                Common.d(e);
                            } catch (NumberFormatException e) {
                                Common.d(e);
                            }
                        }
                    }
                    eventType = xpp.next();
                }
                return true;
            } catch (FileNotFoundException e) {
                activity.showError("Couldn't open file", e);
            } catch (XmlPullParserException e) {
                activity.showError("Couldn't parse book parameters", e);
            } catch (IOException e) {
                activity.showError("Couldn't parse book parameters", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Common.d(e);
                    }
                }
            }


            XmlSerializer serializer = Xml.newSerializer();
            writer = new FileWriter(inputName);
            serializer.setOutput(writer);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startDocument("UTF-8", true);
            String nameSpace = "";
            serializer.startTag(nameSpace, "bookmarks");
            serializer.attribute(nameSpace, "version", "1");
            serializer.attribute(nameSpace, "date", new SimpleDateFormat().format(new Date()));

            int bookIdColumn = c.getColumnIndex(BookmarkAccessor.BOOK_ID);
            //hack
            if (bookIdColumn == -1) {
                String [] names = c.getColumnNames();
                for (int i = 0; i < names.length; i++) {
                    String name = names[i];
                    if (name.endsWith("." + BookmarkAccessor.BOOK_ID)) {
                        bookIdColumn = i;
                        break;
                    }
                }
            }
            int bookName = c.getColumnIndex(BookmarkAccessor.BOOK_NAME);
            int bookSize = c.getColumnIndex(BookmarkAccessor.BOOK_FILE_SIZE);
            int bookmarkPage = c.getColumnIndex(BookmarkAccessor.BOOKMARK_PAGE);
            int bookmarkText = c.getColumnIndex(BookmarkAccessor.BOOKMARK_TEXT);

            long lastBookId = -1;
            do {
                long newBookId = c.getLong(bookIdColumn);

                if (newBookId != lastBookId) {
                    if (lastBookId != -1) {
                        serializer.endTag(nameSpace, "book");
                    }
                    lastBookId = newBookId;
                    serializer.startTag(nameSpace, "book");
                    serializer.attribute(nameSpace, "fileName", c.getString(bookName));
                    serializer.attribute(nameSpace, "fileSize", Long.toString(c.getLong(bookSize)));
                }

                serializer.startTag(nameSpace, "bookmark");
                serializer.attribute(nameSpace, "page", Integer.toString(c.getInt(bookmarkPage) + 1));
                serializer.text(c.getString(bookmarkText));
                serializer.endTag(nameSpace, "bookmark");


            } while (c.moveToNext());

            if (lastBookId != -1) {
                serializer.endTag(nameSpace, "book");
            }

            serializer.endTag(nameSpace, "bookmarks");
            serializer.endDocument();
            serializer.flush();
        } finally {
            if (c != null) {
                c.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        return true;
    }


}
