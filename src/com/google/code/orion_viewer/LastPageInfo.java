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

import java.io.Serializable;

/**
 * User: mike
 * Date: 13.09.11
 * Time: 12:19
 */
class LastPageInfo implements Serializable {
    int pageNumber;
    int rotation;

    int offsetX;
    int offsetY;

    int zoom;

    int leftMargin = 0;
    int rightMargin = 0;
    int topMargin = 0;
    int bottomMargin = 0;

    transient String fileName;
}