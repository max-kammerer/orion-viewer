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
import android.graphics.Point;

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

    private OrionViewerActivity activity;

    private RenderThread renderer;

    private int lastPage = -1;

    private DocumentViewAdapter listener;

    private Point lastScreenSize;

    public static final int ROTATE_90 = -1;
    public static final int ROTATE_270 = 1;

    public Controller(OrionViewerActivity activity, DocumentWrapper doc, LayoutStrategy layout, OrionView view) {
        this.activity = activity;
        this.doc = doc;
        this.layout = layout;
        this.view = view;
        renderer = new RenderThread(activity, view, layout, doc);
        renderer.start();

        listener = new  DocumentViewAdapter() {
            public void viewParametersChanged() {
                renderer.invalidateCache();
                drawPage();
            }
        };

        activity.getSubscriptionManager().addDocListeners(listener);
    }

    public void drawPage(int page) {
        layout.reset(layoutInfo, page);
        drawPage();
    }

    public void drawPage() {
        sendPageChangedNotification();
        renderer.render(layoutInfo);
    }

    public void screenSizeChanged(int newWidth, int newHeight) {
        Common.d("New screen size " + newWidth + "x" + newHeight);
        layout.setDimension(newWidth, newHeight);
        int offsetX = layoutInfo.cellX;
        int offsetY = layoutInfo.cellY;
        layout.reset(layoutInfo, layoutInfo.pageNumber);
        if (lastScreenSize != null) {
            if (newWidth == lastScreenSize.x && newHeight == lastScreenSize.y) {
                layoutInfo.cellX = offsetX;
                layoutInfo.cellY = offsetY;
            }
            lastScreenSize = null;
        }
        sendViewChangeNotification();
        renderer.onResume();
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

    //left, top, right, bottom
    public void changeMargins(int [] margins) {
        changeMargins(margins[0], margins[2], margins[1], margins[3]);
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
        Common.d("Destroying controller...");
        activity.getSubscriptionManager().unSubscribe(listener);

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

//    public void onResume() {
//        Common.d("Controller onResume" + view.getWidth() + "x" + view.getHeight());
//        int oldX = layoutInfo.cellX;
//        int oldY = layoutInfo.cellY;
//        layout.setDimension(view.getWidth(), view.getHeight());
//        layout.reset(layoutInfo, layoutInfo.pageNumber);
//
//        //reset position on changing screen size
//        if (lastScreenSize.x == view.getWidth() && lastScreenSize.y == view.getHeight()) {
//            layoutInfo.cellX = oldX;
//            layoutInfo.cellY = oldY;
//        }
//        drawPage();
//        renderer.onResume();
//        renderer.start();
//    }

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
        layoutInfo.cellX = info.offsetX;
        layoutInfo.cellY = info.offsetY;

        lastScreenSize = new Point(info.screenWidth, info.screenHeight);

        if (view.getWidth() != 0 && view.getHeight() != 0) {
            Common.d("Calling screen size from init...");
            screenSizeChanged(view.getWidth(), view.getHeight());
        }
    }

    public void serialize(LastPageInfo info) {
        layout.serialize(info);
        info.offsetX = layoutInfo.cellX;
        info.offsetY = layoutInfo.cellY;
        info.pageNumber = layoutInfo.pageNumber;
    }

    public void sendViewChangeNotification() {
        activity.getSubscriptionManager().sendViewChangeNotification();
    }

    public void sendPageChangedNotification() {
        if (lastPage != layoutInfo.pageNumber) {
            lastPage = layoutInfo.pageNumber;
            activity.getSubscriptionManager().sendPageChangedNotification(lastPage, doc.getPageCount());
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

    public OrionViewerActivity getActivity() {
        return activity;
    }

}
