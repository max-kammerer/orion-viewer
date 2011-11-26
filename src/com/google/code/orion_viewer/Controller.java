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

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

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

    private int lastPage = -1;

    private List<DocumentViewListener> listeners = new ArrayList<DocumentViewListener>();

    public Controller(OrionViewerActivity activity, DocumentWrapper doc, LayoutStrategy layout, OrionView view) {
        this.doc = doc;
        this.layout = layout;
        this.view = view;
        renderer = new RenderThread(activity, view, layout, doc);
        renderer.start();

        addDocListeners(new  DocumentViewAdapter() {
            public void viewParametersChanged() {
                renderer.invalidateCache();
                drawPage();
            }
        });

    }

    public void drawPage(int page) {
        layout.reset(layoutInfo, page);
        drawPage();
    }

    public void drawPage() {
        sendPageChangedNotification();

        renderer.render(layoutInfo);
    }

    public void drawNext() {
        layout.nextPage(layoutInfo);
        drawPage();
    }

    public void drawPrev() {
        layout.prevPage(layoutInfo);
        drawPage();
    }

    public void changeZoom(int zoom) {
        if (layout.changeZoom(zoom)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            sendViewChangeNotification();
        }
    }

    public int getZoomFactor() {
        return layout.getZoom();
    }


    public void changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        if (layout.changeMargins(leftMargin, topMargin, rightMargin, bottomMargin)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            sendViewChangeNotification();
        }
    }

    public void getMargins(int [] cropMargins) {
        layout.getMargins(cropMargins);
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
            //view.setRotation(rotation);
            sendViewChangeNotification();
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
            layoutInfo.cellX = info.offsetX;
        }
        if (info.offsetY != 0) {
            layoutInfo.cellY = info.offsetY;
        }
        //view.setRotation(layout.getRotation());
    }

    public void serialize(LastPageInfo info) {
        layout.serialize(info);
        info.offsetX = layoutInfo.cellX;
        info.offsetY = layoutInfo.cellY;
        info.pageNumber = layoutInfo.pageNumber;
    }

    public void addDocListeners(DocumentViewListener listeners) {
        this.listeners.add(listeners);
    }

    public void sendViewChangeNotification() {
        for (int i = 0; i < listeners.size(); i++) {
            DocumentViewListener documentListener =  listeners.get(i);
            documentListener.viewParametersChanged();
        }
    }

    public void sendPageChangedNotification() {
        if (lastPage != layoutInfo.pageNumber) {
            lastPage = layoutInfo.pageNumber;
            for (int i = 0; i < listeners.size(); i++) {
                DocumentViewListener documentListener =  listeners.get(i);
                documentListener.pageChanged(lastPage, doc.getPageCount());
            }
        }
    }

    public int getDirection() {
        return layout.getDirection();
    }

    public int getLayout() {
        return layout.getLayout();
    }

    public void setDirectionAndLayout(int navigation, int pageLayout) {
        if (layout.changeNavigation(navigation) | layout.changePageLayout(pageLayout)) {
            sendViewChangeNotification();
        }
    }
}
