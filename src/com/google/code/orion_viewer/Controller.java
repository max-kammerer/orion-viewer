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
 * Time: 18:48
 */
public class Controller {

    private LayoutPosition layoutInfo;

    private DocumentWrapper doc;

    private LayoutStrategy layout;

    private OrionView view;

    private RenderThread renderer;

    public Controller(DocumentWrapper doc, LayoutStrategy layout, OrionView view) {
        this.doc = doc;
        this.layout = layout;
        this.view = view;
        renderer = new RenderThread(view, layout, doc);
        renderer.start();
    }

    public void drawPage(int page) {
        layout.reset(layoutInfo, page);
        drawPage();
    }

    public void drawPage() {
        renderPage(layoutInfo);
    }

    public void drawNext() {
        layout.nextPage(layoutInfo);
        drawPage();
    }

    public void drawPrev() {
        layout.prevPage(layoutInfo);
        drawPage();
    }

    private void renderPage(LayoutPosition info) {
        renderer.render(info);
    }

    public void changeZoom(int zoom) {
        if (layout.changeZoom(zoom)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            renderPage(layoutInfo);
        }
    }

    public int getZoomFactor() {
        return layout.getZoom();
    }


    public void changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        if (layout.changeMargins(leftMargin, topMargin, rightMargin, bottomMargin)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            drawPage();
        }
    }

    public void destroy() {
        if (renderer != null) {
            renderer.stopRenderer();
            renderer = null;
        }
        if (doc != null) {
            doc.destroy();
            doc = null;
        }
        System.gc();
    }

    public void onStart() {
        renderer.onResume();
    }

    public void onPause() {
        renderer.onPause();
    }

    public void setRotation(int rotation) {
        if (layout.changeRotation(rotation)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            view.setRotation(rotation);
            renderPage(layoutInfo);
        }
    }

    public int getRotation() {
        return layout.getRotation();
    }

    public int getCurrentPage() {
        return layoutInfo.pageNumber;
    }

    public int getPageCount() {
        return doc.getPageCount();
    }


    public void init(LastPageInfo info) {
        layout.init(info);
        layoutInfo = new LayoutPosition();
        layout.reset(layoutInfo, info.pageNumber);
        if (info.offsetX != 0) {
            layoutInfo.offsetX = info.offsetX;
        }
        if (info.offsetY != 0) {
            layoutInfo.offsetY = info.offsetY;
        }
        view.setRotation(layout.getRotation());
    }

    public void serialize(LastPageInfo info) {
        layout.serialize(info);
        info.offsetX = layoutInfo.offsetX;
        info.offsetY = layoutInfo.offsetY;
        info.pageNumber = layoutInfo.pageNumber;
    }


}
