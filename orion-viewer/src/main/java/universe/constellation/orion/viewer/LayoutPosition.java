/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
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

import android.graphics.RectF;

import static universe.constellation.orion.viewer.LoggerKt.log;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 18:49
 */
public class LayoutPosition implements Cloneable {

    public OneDimension x = new OneDimension();

    public OneDimension y = new OneDimension();

    public int pageNumber;

    public int screenWidth;

    public int screenHeight;

    public double docZoom;

    public int rotation;

    @Override
    public LayoutPosition clone() {
        try {
            LayoutPosition lp = (LayoutPosition) super.clone();
            lp.x = (OneDimension) lp.x.clone();
            lp.y = (OneDimension) lp.y.clone();
            return lp;
        } catch (CloneNotSupportedException e) {
            log(e);
        }
        return null;//todo new
    }

    @Override
    public boolean equals(Object o) {
        LayoutPosition pos = (LayoutPosition) o;
        if (pos.pageNumber == pageNumber &&
                pos.x.equals(x) &&
                pos.y.equals(y) &&
                pos.rotation == rotation &&
                pos.screenHeight == screenHeight &&
                pos.screenWidth == screenWidth) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pageNumber / 3 + x.hashCode() / 3 + y.hashCode() / 3;
    }


    public void setDocZoom(int zoom) {
        if (zoom <= 0) {
            switch (zoom) {
                //fit width
                case 0: docZoom = ((double) x.screenDimension) / x.pageDimension; break;
                //fit height
                case -1: docZoom = ((double) y.screenDimension) / y.pageDimension; break;
                //fit page
                case -2: docZoom = Math.min(((double) x.screenDimension) / x.pageDimension, ((double) y.screenDimension) / y.pageDimension); break;
            }
        } else {
            docZoom = 0.0001f * zoom;
        }
    }

    @Override
    public String toString() {
        return "page[pageNumber:" + pageNumber + " x: " + x + " y: " + y + "]";
    }

    public RectF toAbsoluteRect() {
        int left = x.offset + x.marginLess;
        int top = y.offset + y.marginLess;
        return new RectF(left, top, left + x.screenDimension, top + y.screenDimension);///
    }
}
