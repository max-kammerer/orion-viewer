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
import org.xmlpull.v1.XmlSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: mike
 * Date: 29.02.12
 * Time: 9:51
 */
public class BookmarkExporter {

    private BookmarkAccessor dataBase;

    private String outputName;

    public BookmarkExporter(BookmarkAccessor dataBase, String outputName) {
        this.dataBase = dataBase;
        this.outputName = outputName;
    }


    /*if bookId null export all*/
    public boolean export(long bookId) throws IOException {

        Cursor c = null;
        FileWriter writer = null;
        try {
            c = dataBase.getExportedBookCursor(bookId);
            if (c == null || !c.moveToFirst()) {
                return  false;
            }

            XmlSerializer serializer = Xml.newSerializer();
            writer = new FileWriter(outputName);
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
