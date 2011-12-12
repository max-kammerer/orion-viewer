package com.google.code.orion_viewer.djvu;

import android.util.Log;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.DocumentWrapper;
import com.google.code.orion_viewer.PageInfo;

import java.util.Date;

/**
 * User: mike
 * Date: 22.11.11
 * Time: 10:42
 */
public class DjvuDocument implements DocumentWrapper {

    static {
		System.loadLibrary("djvu");
	}

    private int pageCount;

    private int lastPage = -1;

    public DjvuDocument(String fileName) {
        openDocument(fileName);
    }

    public boolean openDocument(String fileName) {
        pageCount = openFile(fileName);
        return true;
    }

    public int getPageCount() {
        return pageCount;
    }

    public PageInfo getPageInfo(int pageNum) {
        PageInfo info = new PageInfo();
        getPageInfo(pageNum, info);
        return info;
    }

    public int[] renderPage(int pageNumber, float zoom, int w, int h, int left, int top, int right, int bottom) {
        gotoPage(pageNumber);
        return drawPage((int)(zoom*1000), right - left, bottom - top, left, top, right - left, bottom - top);
    }

    public void destroy() {
        destroying();
    }

    private synchronized void gotoPage(int page) {
        if(lastPage != page) {
            Common.d("Changing page...");
            Date date = new Date();
            if (page > pageCount-1)
                page = pageCount-1;
            else if (page < 0)
                page = 0;
            gotoPageInternal(page);

            Date date2 = new Date();

            lastPage = page;
            Common.d("Change page time " + page + " = " + 0.001 * (date2.getTime() - date.getTime()));
        }
	}


    private static native int openFile(String filename);
	private static native void gotoPageInternal(int localActionPageNum);
	private static native int getPageInfo(int pageNum, PageInfo info);

	public static native int [] drawPage(int zoom1000, int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	public static native void destroying();
}
