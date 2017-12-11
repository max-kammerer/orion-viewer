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

package universe.constellation.orion.viewer.bookmarks;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import universe.constellation.orion.viewer.OrionException;

import java.io.*;
import java.util.Set;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 30.09.12
 * Time: 10:25
 */
public class BookmarkImporter {

    private BookmarkAccessor dataBase;

    private String inputName;

    private Set<BookNameAndSize> books;

    private BookNameAndSize toBook;


    public BookmarkImporter(BookmarkAccessor dataBase, String inputName, Set<BookNameAndSize> books, BookNameAndSize toBook) {
        this.dataBase = dataBase;
        this.inputName = inputName;
        this.books = books;
        this.toBook = toBook;
    }


    public boolean doImport() throws OrionException {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(inputName));

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
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
                        if ("book".equals(name)) {
                            long size = Long.valueOf(xpp.getAttributeValue("", "fileSize"));
                            String fileName = xpp.getAttributeValue("", "fileName");
                            BookNameAndSize book = new BookNameAndSize(fileName, size);
                            if (books.contains(book)) {
                                //next will be called inside
                                eventType = doBookImport(xpp, toBook != null ? toBook : book);
                                continue;
                            }
                        }
                    }
                }
                eventType = xpp.next();
            }
            return true;
        } catch (FileNotFoundException e) {
            throw new OrionException("Couldn't parse book parameters", e);
        } catch (XmlPullParserException e) {
            throw new OrionException("Couldn't parse book parameters", e);
        } catch (IOException e) {
            throw new OrionException("Couldn't parse book parameters" , e);
        } catch (NumberFormatException e) {
            throw new OrionException("Couldn't parse book parameters" , e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log(e);
                }
            }
        }
    }



    //now we stay on book start tag
    private int doBookImport(XmlPullParser xpp, BookNameAndSize book) throws OrionException {
        log("Importing bookmarks for " + book);
        try {
            String bookName = book.getName();
            long bookSize = book.getSize();

            long bookId = dataBase.selectBookId(bookName, bookSize);
            if (bookId == -1) {
                bookId = dataBase.insertOrUpdate(bookName, bookSize);
            }

            if (bookId == -1) {
                throw new DataBaseInsertException("Couldn't insert new book record to database", book);
            }

            int eventType = XmlPullParser.START_TAG;
            int pageNumber = -1;
            while (eventType != XmlPullParser.END_DOCUMENT && !(eventType == XmlPullParser.END_TAG && "book".equals(xpp.getName()))) {

                if (eventType == XmlPullParser.START_TAG ) {
                    String name = xpp.getName();

                    if ("bookmark".equals(name)) {
                        try {
                            pageNumber = Integer.valueOf(xpp.getAttributeValue("", "page")) - 1;
                        } catch (NumberFormatException e){
                            throw new OrionException("Wrong page number for book", book.toString() +": " + xpp.getAttributeValue("", "page"));
                        }
                    }
                }

                if (eventType == XmlPullParser.TEXT && pageNumber != -1) {
                    String text = xpp.getText();
                    if (text != null) {
                        dataBase.insertOrUpdateBookmark(bookId, pageNumber, text);
                    }
                    pageNumber = -1;
                }

                eventType = xpp.next();
            }
            return eventType;
        } catch (XmlPullParserException e) {
            throw new OrionException("Couldn't parse book parameters " + book, e);
        } catch (IOException e) {
            throw new OrionException("Couldn't parse book parameters " + book, e);
        }
    }

}
