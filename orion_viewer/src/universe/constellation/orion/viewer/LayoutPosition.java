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

    public OneDirection x = new OneDirection();

    public OneDirection y = new OneDirection();

    public int pageNumber;

    public int screenWidth;

    public int screenHeight;

    public double docZoom;

    public int rotation;

    @Override
    public LayoutPosition clone() {
        try {
            LayoutPosition lp = (LayoutPosition) super.clone();
            lp.x = (OneDirection) lp.x.clone();
            lp.y = (OneDirection) lp.y.clone();
            return lp;
        } catch (CloneNotSupportedException e) {
            Common.d(e);
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

    public int getRenderWidth() {
        return (rotation == 0 ? x : y).screenDimension;
    }

    public int getRenderHeight() {
        return (rotation == 0 ? y : x).screenDimension;
    }

    public void setDocZoom(int zoom) {
        if (zoom <= 0) {
            switch (zoom) {
                //zoom by width
                case 0: docZoom = ((double) x.screenDimension) / x.pageDimension; break;
                case -1: docZoom = ((double) y.screenDimension) / y.screenDimension; break;
                case -2: docZoom = Math.min(((double) x.screenDimension) / x.pageDimension, ((double) y.screenDimension) / y.screenDimension); break;
            }
        } else {
            docZoom = 0.0001f * zoom;
        }
    }

    public OneDirection getHor() {
        return x;
    }

    public OneDirection getVert() {
        return y;
    }
}
