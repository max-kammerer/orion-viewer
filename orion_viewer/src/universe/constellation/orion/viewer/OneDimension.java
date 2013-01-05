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
 * Date: 01.01.13
 * Time: 18:57
 */
public class OneDimension implements Cloneable {

    public int offset;

    public int overlap;

    public int pageDimension;

    public int screenDimension;

    public int marginLess;

    //not used now
    private int marginTop;

    @Override
    public int hashCode() {
        return offset / 3 + marginLess /3 + pageDimension / 3;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OneDimension) {
            OneDimension other = (OneDimension) o;
            return offset == other.offset &&
                    pageDimension == other.pageDimension &&
                    screenDimension == other.screenDimension &&
                    marginLess == other.marginLess;
        }

        return false;
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
