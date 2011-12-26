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


import android.graphics.Point;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 9:53
 */
public interface LayoutStrategy {

    void nextPage(LayoutPosition pos);

    void prevPage(LayoutPosition pos);

    void reset(LayoutPosition pos, int pageNumber);

    boolean changeRotation(int rotation);

    int getRotation();

    boolean changeZoom(int zoom);

    int getZoom();

    boolean changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin);

    void getMargins(int [] cropMargins);

    void init(LastPageInfo info);

    void serialize(LastPageInfo info);

    Point convertToPoint(LayoutPosition pos);

    int getLayout();

    int getDirection();

    boolean changeNavigation(int navigation);

    boolean changePageLayout(int navigation);

    void setDimension(int width, int height);

}
