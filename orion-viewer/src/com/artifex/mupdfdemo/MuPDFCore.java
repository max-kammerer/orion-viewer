package com.artifex.mupdfdemo;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PointF;
import android.graphics.RectF;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.DocInfo;
import universe.constellation.orion.viewer.PageInfo;

import java.util.ArrayList;


public class MuPDFCore
{
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}

	/* Readable members */
	private int numPages = -1;
	private float pageWidth;
	private float pageHeight;
	private long globals;
	private byte fileBuffer[];
	private String file_format;
    private DocInfo info;

	/* The native functions */
    private native long openFile(String filename, DocInfo info);
	private native long openBuffer();
	private native String fileFormatInternal();
	private native int countPagesInternal();
	private native void gotoPageInternal(int localActionPageNum);
	private native float getPageWidth();
	private native float getPageHeight();
	private native void drawPage(Bitmap bitmap,
                                 float zoom,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native void updatePageInternal(Bitmap bitmap,
			int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	private native RectF[] searchPage(String text);
	private native TextChar[][][][] text();
	private native byte[] textAsHtml();
	private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);
	private native void addInkAnnotationInternal(PointF[][] arcs);
	private native void deleteAnnotationInternal(int annot_index);
	private native int passClickEventInternal(int page, float x, float y);
	private native void setFocusedWidgetChoiceSelectedInternal(String [] selected);
	private native String [] getFocusedWidgetChoiceSelected();
	private native String [] getFocusedWidgetChoiceOptions();
	private native int setFocusedWidgetTextInternal(String text);
	private native String getFocusedWidgetTextInternal();
	private native int getFocusedWidgetTypeInternal();
	private native LinkInfo [] getPageLinksInternal(int page);
	private native RectF[] getWidgetAreasInternal(int page);
	private native Annotation[] getAnnotationsInternal(int page);
	private native OutlineItem [] getOutlineInternal();
	private native boolean hasOutlineInternal();
	private native boolean needsPasswordInternal();
	private native boolean authenticatePasswordInternal(String password);
	private native MuPDFAlertInternal waitForAlertInternal();
	private native void replyToAlertInternal(MuPDFAlertInternal alert);
	private native void startAlertsInternal();
	private native void stopAlertsInternal();
	private native void destroying();
	private native boolean hasChangesInternal();
	private native void saveInternal();

	public static native boolean javascriptSupported();

	public MuPDFCore(String filename) throws Exception
	{
        info = new DocInfo();
        globals = openFile(filename, info);
		if (globals == 0)
		{
			throw new Exception(String.format("Cannot open file", filename));
		}
		file_format = fileFormatInternal();
	}

	public MuPDFCore(byte buffer[]) throws Exception
	{
		fileBuffer = buffer;
		globals = openBuffer();
		if (globals == 0)
		{
			throw new Exception("Cannot open pdf from buffer");
		}
		file_format = fileFormatInternal();
	}

	public  int countPages()
	{
		if (numPages < 0)
			numPages = countPagesSynchronized();

		return numPages;
	}

	public String fileFormat()
	{
		return file_format;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	private void gotoPage(int page)
	{
        Common.d("Changing page to " + page + " ...");
        long start = System.currentTimeMillis();

		if (page > numPages-1)
			page = numPages-1;
		else if (page < 0)
			page = 0;
		gotoPageInternal(page);
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();

        Common.d("Page " + page + " changing takes = " + 0.001 * (System.currentTimeMillis() - start) + " s");
	}

	public synchronized PointF getPageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}

	public MuPDFAlert waitForAlert() {
		MuPDFAlertInternal alert = waitForAlertInternal();
		return alert != null ? alert.toAlert() : null;
	}

	public void replyToAlert(MuPDFAlert alert) {
		replyToAlertInternal(new MuPDFAlertInternal(alert));
	}

	public void stopAlerts() {
		stopAlertsInternal();
	}

	public void startAlerts() {
		startAlertsInternal();
	}

	public synchronized void onDestroy() {
        Common.d("Destroying document...");
		destroying();
		globals = 0;
        Common.d("Document destoyed!");
	}

	public synchronized Bitmap drawPage(int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		gotoPage(page);
		Bitmap bm = Bitmap.createBitmap(patchW, patchH, Config.ARGB_8888);
		drawPage(bm, 1f, pageW, pageH, patchX, patchY, patchW, patchH);
		return bm;
	}

	public synchronized Bitmap updatePage(BitmapHolder h, int page,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		Bitmap bm = null;
		Bitmap old_bm = h.getBm();

		if (old_bm == null)
			return null;

		bm = old_bm.copy(Bitmap.Config.ARGB_8888, false);
		old_bm = null;

		updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH);
		return bm;
	}

//	public synchronized PassClickResult passClickEvent(int page, float x, float y) {
//		boolean changed = passClickEventInternal(page, x, y) != 0;
//
//		switch (WidgetType.values()[getFocusedWidgetTypeInternal()])
//		{
//		case TEXT:
//			return new PassClickResultText(changed, getFocusedWidgetTextInternal());
//		case LISTBOX:
//		case COMBOBOX:
//			return new PassClickResultChoice(changed, getFocusedWidgetChoiceOptions(), getFocusedWidgetChoiceSelected());
//		default:
//			return new PassClickResult(changed);
//		}
//
//	}

	public synchronized boolean setFocusedWidgetText(int page, String text) {
		boolean success;
		gotoPage(page);
		success = setFocusedWidgetTextInternal(text) != 0 ? true : false;

		return success;
	}

	public synchronized void setFocusedWidgetChoiceSelected(String [] selected) {
		setFocusedWidgetChoiceSelectedInternal(selected);
	}

	public synchronized LinkInfo [] getPageLinks(int page) {
		return getPageLinksInternal(page);
	}

	public synchronized RectF [] getWidgetAreas(int page) {
		return getWidgetAreasInternal(page);
	}

	public synchronized Annotation [] getAnnoations(int page) {
		return getAnnotationsInternal(page);
	}

	public synchronized RectF [] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized byte[] html(int page) {
		gotoPage(page);
		return textAsHtml();
	}

    public synchronized String textLines(int page, RectF region) {
        gotoPage(page);
        TextChar[][][][] chars = text();

        // The text of the page held in a hierarchy (blocks, lines, spans).
        // Currently we don't need to distinguish the blocks level or
        // the spans, and we need to collect the text into words.
        ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();


        for (TextChar[][][] bl: chars) {
            if (bl != null) {
                for (TextChar[][] ln: bl) {
                    if (ln != null) {
                        ArrayList<TextWord> wds = new ArrayList<TextWord>();
                        TextWord wd = new TextWord();
                        for (TextChar[] sp: ln) {
                            for (TextChar tc: sp) {
                                if (tc.c != ' ') {
                                    wd.Add(tc);
                                } else if (wd.textLength() > 0) {
                                    float square = wd.width() * wd.height() / 5;
                                    if (wd.setIntersect(wd, region)) {
                                        if (wd.width() * wd.height() > square) {
                                            wds.add(wd);
                                            System.out.println(wd.getText());
                                        }
                                    }
                                    wd = new TextWord();
                                }
                            }

                            if (wd.textLength() > 0) {
                                float square = wd.width() * wd.height() / 5;
                                if (wd.setIntersect(wd, region)) {
                                    if (wd.width() * wd.height() > square) {
                                        wds.add(wd);
                                    }
                                }
                            }

                            if (wds.size() > 0)
                                lns.add(wds.toArray(new TextWord[wds.size()]));
                        }
                    }
                }
            }
        }

        StringBuffer res = new StringBuffer();
        for (int i = 0; i < lns.size(); i++) {
            TextWord[] textWords = lns.get(i);
            for (int j = 0; j < textWords.length; j++) {
                TextWord textWord = textWords[j];
                res.append(textWord.getText());
                if (j != textWords.length - 1) {
                    res.append(" ");
                }
            }
            if (i != lns.size() - 1) {
                res.append(" ");
            }
        }
        return res.toString();
    }

	public synchronized void addMarkupAnnotation(int page, PointF[] quadPoints, Annotation.Type type) {
		gotoPage(page);
		addMarkupAnnotationInternal(quadPoints, type.ordinal());
	}

	public synchronized void addInkAnnotation(int page, PointF[][] arcs) {
		gotoPage(page);
		addInkAnnotationInternal(arcs);
	}

	public synchronized void deleteAnnotation(int page, int annot_index) {
		gotoPage(page);
		deleteAnnotationInternal(annot_index);
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized OutlineItem [] getOutline() {
		return getOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}

	public synchronized boolean hasChanges() {
		return hasChangesInternal();
	}

	public synchronized void save() {
		saveInternal();
	}

    public native void setContrast(int contrast);
    public native void setThreshold(int threshold);
    private native void getPageInfo(int pageNum, PageInfo info);

    public synchronized PageInfo getPageInfo(int page) {
        PageInfo info = new PageInfo();
        getPageInfo(page, info);
        System.out.println("Page info: " + info.width + "x" + info.height);
        return info;
    }

    public void freeMemory() {
        onDestroy();
    }

    public DocInfo getInfo() {
        return info;
    }

    public int getPageCount() {
        return info.getPageCount();
    }

    public synchronized void renderPage(int n, Bitmap bitmap, double zoom, int left, int top, int w, int h) {
        gotoPage(n);
        Common.d("MuPDFCore starts rendering...");
        long date = System.currentTimeMillis();
        drawPage(bitmap, (float)zoom, w, h, left, top, w, h);
        long date2 = System.currentTimeMillis();
        Common.d("MuPDFCore render time takes " + n + " = " + 0.001 * (date2 - date) + " s");
    }

}
