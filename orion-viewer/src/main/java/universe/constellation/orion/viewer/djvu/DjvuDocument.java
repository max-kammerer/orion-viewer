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

package universe.constellation.orion.viewer.djvu;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.document.Document;
import universe.constellation.orion.viewer.document.OutlineItem;
import universe.constellation.orion.viewer.PageInfo;
import universe.constellation.orion.viewer.pdf.DocInfo;

/**
 * User: mike
 * Date: 22.11.11
 * Time: 10:42
 */
public class DjvuDocument implements Document {

    static {
		System.loadLibrary("djvu");
	}

    private int lastPage = -1;
    private long doc = 0;
    private long context = 0;
    private final DocInfo docInfo = new DocInfo();

    @Nullable
    @Override
    public OutlineItem[] getOutline() {
        return getOutline(doc);
    }

    public DjvuDocument(String fileName) {
        context = initContext();
        docInfo.fileName = fileName;
        if (context == 0) throw new RuntimeException("Can't create djvu context");
        doc = openFile(fileName, docInfo, context);
        if (doc == 0) throw new RuntimeException("Can't open file " + fileName);
    }

    public int getPageCount() {
        return docInfo.pageCount;
    }

    public PageInfo getPageInfo(int pageNum, int cropMode) {
        PageInfo info = new PageInfo(pageNum);
        long start = System.currentTimeMillis();
        getPageInfo(doc, pageNum, info);
        Common.d("Page " + pageNum + " info takes = " + 0.001 * (System.currentTimeMillis() - start) + " s");
        return info;
    }

    public synchronized void renderPage(int pageNumber, Bitmap bitmap, double zoom, int left, int top, int right, int bottom) {
        gotoPage(pageNumber);
        long start = System.currentTimeMillis();
        drawPage(doc, bitmap, (float) zoom, right - left, bottom - top, left, top, right - left, bottom - top);
        Common.d("Page " + pageNumber + " rendering takes = " + 0.001 * (System.currentTimeMillis() - start) + " s");
    }

    public synchronized void destroy() {
        destroying(doc, context);
    }

    private synchronized void gotoPage(int page) {
        if(lastPage != page) {
            Common.d("Changing page...");
            long start = System.currentTimeMillis();
            if (page > docInfo.pageCount - 1)
                page = docInfo.pageCount - 1;
            else if (page < 0)
                page = 0;
            gotoPageInternal(doc, page);

            lastPage = page;
            Common.d("Page changing takes " + page + " = " + 0.001 * (System.currentTimeMillis() - start));
        }
	}

    private static native long initContext();
    private static native long openFile(String filename, DocInfo info, long context);
	private static synchronized native void gotoPageInternal(long doc, int localActionPageNum);
	private static native int getPageInfo(long doc, int pageNum, PageInfo info);

	public static synchronized  native boolean drawPage(long doc, Bitmap bitmap, float zoom, int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	public static synchronized  native void destroying(long doc, long context);

    public String getTitle() {
        return null;
    }

    public native void setContrast(int contrast);

	public native void setThreshold(int threshold);

	public native OutlineItem[] getOutline(long doc);

    public native String getText(long doc, int pageNumber, int absoluteX, int absoluteY, int width, int height);

    private static synchronized native boolean getPageText(long doc, int pageNumber, ArrayList stringBuilder, ArrayList positions);

    @Override
    public boolean needPassword() {
        return false;
    }

    @Override
    public boolean authenticate(String password) {
        return true;
    }

    @Override
    public RectF[] searchPage(int pageNumber, String text) {
        text = text.toLowerCase();

        ArrayList<String> strings = new ArrayList(500);
        ArrayList<RectF> positions = new ArrayList(500);
        getPageText(doc, pageNumber, strings, positions);

        int prevIndex = 0;
        ArrayList<Integer> indexes = new ArrayList<Integer>(500);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < positions.size(); i++) {
            String string = strings.get(i);
            builder.append(string.toLowerCase());
            int length = builder.length();
            for (int j = prevIndex; j < length; j++) {
                indexes.add(i);
            }
            prevIndex = length;
        }

        int searchFrom = 0;
        ArrayList<RectF> result = new ArrayList<RectF>();
        int i = builder.indexOf(text, searchFrom);
        while (i != -1) {
            Integer start = indexes.get(i);
            Integer end = indexes.get(i + text.length() - 1);

            RectF rectF = new RectF(getSafeRectInPosition(positions, start));
            rectF.union(getSafeRectInPosition(positions, end));
            result.add(rectF);
            i = i + text.length();
            i = builder.indexOf(text, i);
        }

        return result.toArray(new RectF[result.size()]);
    }

    private RectF getSafeRectInPosition(List<RectF> rects, int position) {
        //TODO
        return rects.get(position);
    }

    @Override
    public String getText(int pageNumber, int absoluteX, int absoluteY, int width, int height, boolean singleWord) {
        return getText(doc, pageNumber, absoluteX, absoluteY, width, height);
    }

    @Override
    public boolean hasCalculatedPageInfo(int pageNumber) {
        return false;
    }
}
