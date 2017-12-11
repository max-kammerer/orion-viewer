///*
// * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
// *
// * Copyright (C) 2011-2012  Michael Bogdanov
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package universe.constellation.orion.viewer.bookmarks;
//
//import android.os.AsyncTask;
//import universe.constellation.orion.viewer.AndroidLogger;
//import universe.constellation.orion.viewer.R;
//
//import java.io.File;
//import java.io.IOException;
//
///**
// * User: mike
// * Date: 30.09.12
// * Time: 13:35
// */
//public class BookmarkExportTask implements AsyncTask<Long, Integer, Boolean> {
//
//    private static final int EXPORT_CURRENT = 1;
//    private static final int EXPORT_ALL = 2;
//
//    private boolean showEmptyResult;
//    private int operation;
//    private long currentBookId;
//    private String output;
//    private Exception ex;
//
//    public BookmarkExportTask(boolean showEmptyResult, int operation, long currentBookId, String output) {
//        this.showEmptyResult = showEmptyResult;
//        this.operation = operation;
//        this.currentBookId = currentBookId;
//        this.output = output;
//    }
//
//    @Override
//    protected Boolean doInBackground(Long... params) {
//        if (!showEmptyResult) {
//            long bookId = operation == EXPORT_ALL ? -1 : currentBookId;
//            File file = file + "." + (bookId == -1 ? "all_" : "") +  "bookmarks.xml";
//            log("Bookmarks output file: " + file);
//
//            if (new File(file).exists()) {
//                if (!showAlert("File already exists!", "File " + file + " already exists. Do you want to overwrite it?")) {
//                    return true;
//                }
//            }
//
//            BookmarkExporter exporter = new BookmarkExporter(getOrionContext().getBookmarkAccessor(), file);
//            try {
//                showEmptyResult = !exporter.export(bookId);
//            } catch (IOException e) {
//                ex = e;
//                showError(e);
//                return false;
//            }
//        }
//    }
//
//
//}
