package com.artifex.mupdfdemo;

import android.graphics.RectF;

import universe.constellation.orion.viewer.PageInfo;
import universe.constellation.orion.viewer.document.Page;

public class SearchTaskResult {
	public final String txt;
	public final int pageNumber;
	public final RectF[] searchBoxes;

	public final PageInfo info;

	public final Page page;

	public SearchTaskResult(String _txt, int _pageNumber, RectF _searchBoxes[], PageInfo info, Page page) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
		this.info = info;
		this.page = page;
	}
}
