package com.google.code.orion_viewer;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

/**
 * User: mike
 * Date: 15.10.11
 * Time: 9:53
 */
public interface DocumentWrapper {

    boolean openDocument(String fileName);

    int getPageCount();

    PageInfo getPageInfo(int pageNum);

    int[] renderPage(int pageNumber, float zoom, int w, int h, int left, int top, int right, int bottom);

	void destroy();

    String getTitle();
}
