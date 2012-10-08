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

package universe.constellation.orion.viewer.bookmarks;

import universe.constellation.orion.viewer.OrionBookmarkActivity;

/**
* User: mike
* Date: 07.10.12
* Time: 19:22
*/
public class BookNameAndSize implements Comparable<BookNameAndSize> {

    private String name;

    private long size;

    private long id;

    public BookNameAndSize(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public int compareTo(BookNameAndSize another) {
        int res = name.compareTo(another.name);
        if (res == 0) {
            res = size < another.size ? -1 : (size == another.size ? 0 : 1);
        }
        return res;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getId() {
        return id;
    }

    public String buityfySize() {
        if (size < 1024) {
            return size + "b";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + "." + (size % 1024)/103 + "Kb";
        }
        if (size < 1024 * 1024 * 1024) {
            return (size / (1024 * 1024)) + "." + (size % (1024 * 1024))/(103*1024) + "Mb";
        }
        return size + "b";
    }

    @Override
    public String toString() {
        return name + " " + buityfySize();
    }

    @Override
    public boolean equals(Object o) {
        BookNameAndSize another = (BookNameAndSize) o;
        return size == another.size && name.equals(another.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
