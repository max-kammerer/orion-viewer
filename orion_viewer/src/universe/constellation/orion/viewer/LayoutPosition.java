/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 18:49
 */
public class LayoutPosition implements Cloneable {

    public int pageNumber;

    public int pieceWidth;

    public int pieceHeight;

    public int maxX;

    public int maxY;

    public int cellX;

    public int cellY;

    public int pageWidth;

    public int pageHeight;

    public int screenWidth;

    public int screenHeight;

    public int marginLeft;

    public int marginTop;

    public double docZoom;

    //public int rotation;

    @Override
    public LayoutPosition clone() {
        try {
            return (LayoutPosition) super.clone();
        } catch (CloneNotSupportedException e) {
            Common.d(e);
        }
        return null;//todo new
    }

    @Override
    public boolean equals(Object o) {
        LayoutPosition pos = (LayoutPosition) o;
        if (pos.pageNumber == pageNumber &&
                pos.cellX == cellX &&
                pos.cellY == cellY &&
                pos.pieceWidth == pieceWidth &&
                pos.pieceHeight == pieceHeight) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pageNumber / 3 + cellX / 3 + cellY / 3;
    }
}
