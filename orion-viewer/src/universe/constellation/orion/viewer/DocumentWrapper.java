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

import android.graphics.Bitmap;
import universe.constellation.orion.viewer.outline.OutlineItem;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 9:53
 */
public interface DocumentWrapper {

    boolean openDocument(String fileName);

    int getPageCount();

    PageInfo getPageInfo(int pageNum);

    void renderPage(int pageNumber, Bitmap bitmap, double zoom, int w, int h, int left, int top, int right, int bottom);

    String getText(int pageNumber, int absoluteX, int absoluteY, int width, int height);

	void destroy();

    String getTitle();

	void setContrast(int contrast);

	void setThreshold(int threshold);
	
	public OutlineItem[] getOutline();

    public boolean needPassword();

    public boolean authentificate(String password);
}
