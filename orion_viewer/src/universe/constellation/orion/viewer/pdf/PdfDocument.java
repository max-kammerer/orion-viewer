package universe.constellation.orion.viewer.pdf;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
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

import com.artifex.mupdf.MuPDFCore;
import universe.constellation.orion.viewer.AbstractDocumentWrapper;
import universe.constellation.orion.viewer.DocumentWrapper;
import universe.constellation.orion.viewer.PageInfo;

/**
 * User: mike
 * Date: 16.10.11
 * Time: 17:57
 */
public class PdfDocument extends AbstractDocumentWrapper {

    private MuPDFCore core;

    public PdfDocument(String fileName) throws Exception {
        core = new MuPDFCore(fileName);
    }

    public boolean openDocument(String fileName) {
        return false;
    }

    public int getPageCount() {
        return core.getPageCount();
    }

    public PageInfo getPageInfo(int pageNum) {
        return core.getPageInfo(pageNum);
    }

    public int[] renderPage(int pageNumber, double zoom, int w, int h, int left, int top, int right, int bottom) {
        return core.renderPage(pageNumber, zoom, left, top, right - left, bottom - top);
    }

    public void destroy() {
        core.freeMemory();
    }

    public String getTitle() {
        return core.getInfo().title;
    }
}
