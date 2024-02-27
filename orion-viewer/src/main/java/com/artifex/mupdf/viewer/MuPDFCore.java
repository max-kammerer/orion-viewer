package com.artifex.mupdf.viewer;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.artifex.mupdf.fitz.Device;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.StructuredText;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

public class MuPDFCore
{
	private Document doc;
	private Outline[] outline;
	private int pageCount = -1;
	private int currentPage;
	private Page page;
	private float pageWidth;
	private float pageHeight;
	private DisplayList displayList;

	/* Default to "A Format" pocket book size. */
	private int layoutW = 312;
	private int layoutH = 504;
	private int layoutEM = 10;

	public MuPDFCore(String filename) {
		doc = Document.openDocument(filename);
		//doc.layout(layoutW, layoutH, layoutEM);
		pageCount = doc.countPages();
		currentPage = -1;
	}

	public MuPDFCore(byte buffer[], String magic) {
		doc = Document.openDocument(buffer, magic);
		//doc.layout(layoutW, layoutH, layoutEM);
		pageCount = doc.countPages();
		currentPage = -1;
	}

	public synchronized String getTitle() {
		return doc.getMetaData(Document.META_INFO_TITLE);
	}

	public int countPages() {
		return pageCount;
	}

	public synchronized boolean isReflowable() {
		return doc.isReflowable();
	}

	public synchronized Location layout(Location oldPage, int w, int h, int em) {
		if (w != layoutW || h != layoutH || em != layoutEM) {
			System.out.println("LAYOUT: " + w + "," + h);
			layoutW = w;
			layoutH = h;
			layoutEM = em;
			long mark = doc.makeBookmark(oldPage);
			doc.layout(layoutW, layoutH, layoutEM);
			currentPage = -1;
			pageCount = doc.countPages();
			outline = null;
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
			return doc.findBookmark(mark);
		}
		return oldPage;
	}

	public synchronized void gotoPage(int pageNum) {
		/* TODO: page cache */
		if (pageNum > pageCount-1)
			pageNum = pageCount-1;
		else if (pageNum < 0)
			pageNum = 0;
		if (pageNum != currentPage) {
			if (page != null)
				page.destroy();
			page = null;
			if (displayList != null)
				displayList.destroy();
			displayList = null;
			page = null;
			pageWidth = 0;
			pageHeight = 0;
			currentPage = -1;

			if (doc != null) {
				page = doc.loadPage(pageNum);
				Rect b = page.getBounds();
				pageWidth = b.x1 - b.x0;
				pageHeight = b.y1 - b.y0;
			}

			currentPage = pageNum;
		}
	}

	public synchronized PointF getPageSize(int pageNum) {
		//destroyed, can be called in non-ui thread
		if (doc == null) return new PointF(300, 400);
		gotoPage(pageNum);
		return new PointF(pageWidth, pageHeight);
	}

	public synchronized void onDestroy() {
		if (displayList != null)
			displayList.destroy();
		displayList = null;
		if (page != null)
			page.destroy();
		page = null;
		if (doc != null)
			doc.destroy();
		doc = null;
	}

	public synchronized void drawPage(Bitmap bitmap, int pageNum,
			int left, int top,
			int right, int bottom,
			float zoom) {
		//destroyed, can be called in non-ui thread
		if (doc == null) return;
		gotoPage(pageNum);

		if (displayList == null && page != null)
			try {
				displayList = page.toDisplayList();
			} catch (Exception ex) {
				displayList = null;
			}

		if (displayList == null || page == null)
			return;

		Device dev = new AndroidDrawDevice(bitmap, left, top, 0, 0, right - left, bottom - top, true);
		try {
			displayList.run(dev, new Matrix(zoom, zoom), null);
			dev.close();
		} finally {
			dev.destroy();
		}
	}

	public synchronized void drawPage(Bitmap bitmap, int pageNum,
									  int originX, int originY, int patchX0, int patchY0,
									  int patchX1, int patchY1,
									  float zoom) {
		//destroyed, can be called in non-ui thread
		if (doc == null) return;
		gotoPage(pageNum);

		if (displayList == null && page != null)
			try {
				displayList = page.toDisplayList();
			} catch (Exception ex) {
				displayList = null;
			}

		if (displayList == null || page == null)
			return;

		Device dev = new AndroidDrawDevice(bitmap, originX, originY, patchX0, patchY0, patchX1, patchY1);
		try {
			displayList.run(dev, new Matrix(zoom, zoom), null);
			dev.close();
		} finally {
			dev.destroy();
		}
	}

	public synchronized Link[] getPageLinks(int pageNum) {
		gotoPage(pageNum);
		return page != null ? page.getLinks() : null;
	}

	public synchronized Quad[] searchPage(int pageNum, String text) {
		gotoPage(pageNum);
		Quad[][] quads = page.search(text);
		if (quads == null)
			return null;

		int length = 0;
		for (Quad[] quad : quads) {
			length += quad.length;
		}

		Quad[] flattenQuads = new Quad[length];
		int dest=0;
		for(Quad[] qd: quads){
			System.arraycopy(qd, 0, flattenQuads, dest, qd.length);
			dest += qd.length;
		}
		return flattenQuads;
	}

	public synchronized StructuredText getText(int pageNum) {
		gotoPage(pageNum);
		return page.toStructuredText();
	}

	public synchronized boolean hasOutline() {
		if (outline == null) {
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
		}
		return outline != null;
	}

	public synchronized Outline[] getOutline() {
		hasOutline();
		return outline;
	}

	public synchronized boolean needsPassword() {
		return doc.needsPassword();
	}

	public synchronized boolean authenticatePassword(String password) {
		return doc.authenticatePassword(password);
	}

	/**
	 +	 * The convenience method to get page number from raw outline.
	 +	 * @param ol the outline to get the page for.
	 +	 * @return the page number.
	 +	 */
 	public int pageNumberFromOutline(Outline ol){
		Location loc = doc.resolveLink(ol);
		return doc.pageNumberFromLocation(loc);
	}

	public Page getPage() {
		return page;
	}
}
