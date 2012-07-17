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

   	public OutlineItem(int _level, String _title, int _page) {
   		level = _level;
   		title = _title;
   		page  = _page;
   	}

}
