package com.artifex.mupdf;

/**
 * User: mike
 * Date: 15.07.12
 * Time: 19:23
 */
public class OutlineItem {
    public final int    level;
   	public final String title;
   	public final int    page;

   	public OutlineItem(int level, String title, int page) {
   		this.level = level;
        this.title = title;
        this.page  = page;
   	}
}
