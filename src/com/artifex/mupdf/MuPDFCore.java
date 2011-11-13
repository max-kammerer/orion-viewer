package com.artifex.mupdf;

import android.util.Log;
import com.google.code.orion_viewer.Common;
import com.google.code.orion_viewer.PageInfo;

import java.util.Date;

public class MuPDFCore
{
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}

	/* Readable members */
	public int pageNum;
	public int numPages;
    public int lastPage = -1;

	/* The native functions */
	private static native int openFile(String filename);
	private static native void gotoPageInternal(int localActionPageNum);
	private static native int getPageInfo(int pageNum, PageInfo info);

	public static native int [] drawPage(int zoom1000, int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	public static native void destroying();

	public MuPDFCore(String filename) throws Exception
	{
		numPages = openFile(filename);
		if (numPages <= 0) {
			throw new Exception("Failed to open " + filename);
		}
		pageNum = 0;
	}

	private synchronized void gotoPage(int page) {
        if(lastPage != page) {
            Date date = new Date();
            if (page > numPages-1)
                page = numPages-1;
            else if (page < 0)
                page = 0;
            gotoPageInternal(page);

            Date date2 = new Date();
            Log.d(Common.LOGTAG, "Change page time " + page + " = " + 0.001 * (date2.getTime() - date.getTime()));
            this.pageNum = page;
            lastPage = page;
        }
	}

    public synchronized int[] renderPage(int n, float zoom, int left, int top, int w, int h) {
        gotoPage(n);

        Date date = new Date();

        int [] res =  drawPage((int) (zoom * 1000), w, h, left, top, w, h);

        Date date2 = new Date();
        Log.d(Common.LOGTAG, "Total render time " + n + " = " + 0.001 * (date2.getTime() - date.getTime()));
        return res;
    }

	public void onDestroy() {
		destroying();
	}

    public int getPageCount() {
        return numPages;
    }

    public synchronized PageInfo getPageInfo(int page) {
        PageInfo info = new PageInfo();
        getPageInfo(page, info);
        return info;
    }

    public void freeMemory() {
        onDestroy();
    }
}
