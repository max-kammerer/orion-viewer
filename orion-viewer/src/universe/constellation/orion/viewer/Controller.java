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

import android.graphics.Point;

import universe.constellation.orion.viewer.outline.OutlineItem;
import universe.constellation.orion.viewer.prefs.GlobalOptions;
import universe.constellation.orion.viewer.util.ColorUtil;
import universe.constellation.orion.viewer.view.Renderer;
import universe.constellation.orion.viewer.view.ViewDimensionAware;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 18:48
 */
public class Controller implements ViewDimensionAware {

    private LayoutPosition layoutInfo;

    private DocumentWrapper doc;

    private LayoutStrategy layout;

    private OrionViewerActivity activity;

    private Renderer renderer;

    private int lastPage = -1;

    private DocumentViewAdapter listener;

    private String screenOrientation;

    private Point lastScreenSize;

    private int contrast;
    private int threshold;

    private boolean hasPendingEvents = false;

    public Controller(OrionViewerActivity activity, DocumentWrapper doc, LayoutStrategy layout, Renderer renderer) {
        Common.d("Controller created");
        this.activity = activity;
        this.doc = doc;
        this.layout = layout;
        this.renderer = renderer;

        renderer.startRenreder();

        listener = new  DocumentViewAdapter() {
            public void viewParametersChanged() {
                if (Controller.this.activity.isResumed) {
                    Controller.this.renderer.invalidateCache();
                    drawPage(layoutInfo);
                    hasPendingEvents = false;
                } else {
                    hasPendingEvents = true;
                }
            }
        };

        //activity.getOrionContext().getOptions().subscribe(prefListener);
        activity.getSubscriptionManager().addDocListeners(listener);
    }

    public void drawPage(int page) {
        layout.reset(layoutInfo, page);
        drawPage(layoutInfo);
    }

    public void drawPage() {
        drawPage(layoutInfo);
    }

    public void drawPage(LayoutPosition info) {
        layoutInfo = info;
        sendPageChangedNotification();
        renderer.render(info);
    }

    public void processPendingEvents() {
        if (hasPendingEvents) {
            Common.d("Processing pending updates...");
            sendViewChangeNotification();
        }
    }

    public void onDimensionChanged(int newWidth, int newHeight) {
        if (newWidth >= 0 && newHeight >= 0) {
            Common.d("New screen size " + newWidth + "x" + newHeight);

            layout.setDimension(newWidth, newHeight);
            GlobalOptions options = getActivity().getGlobalOptions();
            layout.changeOverlapping(options.getHorizontalOverlapping(), options.getVerticalOverlapping());
            int offsetX = layoutInfo.x.offset;
            int offsetY = layoutInfo.y.offset;
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            if (lastScreenSize != null) {
                if (newWidth == lastScreenSize.x && newHeight == lastScreenSize.y) {
                    layoutInfo.x.offset = offsetX;
                    layoutInfo.y.offset = offsetY;
                }
                lastScreenSize = null;
            }
            sendViewChangeNotification();
            renderer.onResume();

            //HACK
            activity.myprocessOnActivityVisible();
        }
    }

    public void drawNext() {
        layout.nextPage(layoutInfo);
        drawPage(layoutInfo);
    }

    public void drawPrev() {
        layout.prevPage(layoutInfo);
        drawPage(layoutInfo);
    }

    public void translateAndZoom(boolean changeZoom, float zoomScaling, float deltaX, float deltaY) {
        Common.d("zoomscaling  " + changeZoom + " " + zoomScaling + "  " + deltaX + "  " + deltaY );
        int oldOffsetX = layoutInfo.x.offset;
        int oldOffsetY = layoutInfo.y.offset;
        System.out.println("oldZoom  " + layoutInfo.docZoom + "  " + layoutInfo.x.offset + " x " + layoutInfo.y.offset);

        if (changeZoom) {
            layout.changeZoom((int) (10000f * zoomScaling * layoutInfo.docZoom));
            layout.reset(layoutInfo, layoutInfo.pageNumber);
        }

        layoutInfo.x.offset = (int) (zoomScaling * oldOffsetX + deltaX);
        layoutInfo.y.offset = (int) (zoomScaling * oldOffsetY + deltaY);
        System.out.println("newZoom  " + layoutInfo.docZoom + "  " + layoutInfo.x.offset + " x " + layoutInfo.y.offset);

        sendViewChangeNotification();
    }

    public void changeZoom(int zoom) {
        if (layout.changeZoom(zoom)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            sendViewChangeNotification();
        }
    }

    public int getZoom10000Factor() {
        return layout.getZoom();
    }

     public double getCurrentPageZoom() {
         return layoutInfo.docZoom;
     }

    //left, top, right, bottom
    public void changeMargins(int [] margins) {
        changeMargins(margins[0], margins[2], margins[1], margins[3], layout.isEnableEvenCrop(), margins[4], margins[5]);
    }

    public void changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin, boolean isEven, int leftEvenMargin, int rightEvenMargin) {
        if (layout.changeMargins(leftMargin, topMargin, rightMargin, bottomMargin, isEven, leftEvenMargin, rightEvenMargin)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
            sendViewChangeNotification();
        }
    }

    public void getMargins(int [] cropMargins) {
        layout.getMargins(cropMargins);
    }

    public boolean isEvenCropEnabled() {
        return layout.isEnableEvenCrop();
    }

    public void destroy() {
        Common.d("Destroying controller...");
        activity.getSubscriptionManager().unSubscribe(listener);
        //activity.getOrionContext().getOptions().unsubscribe(prefListener);

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
            sendViewChangeNotification();
        }
    }

    public void changeOverlap(int horizontal, int vertical) {
        if (layout.changeOverlapping(horizontal, vertical)) {
            layout.reset(layoutInfo, layoutInfo.pageNumber);
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


    public void init(LastPageInfo info, Point dimension) {
        int width = dimension.x;
        int height = dimension.y;
        Common.d("Init controller...");
        doc.setContrast(info.contrast);
        doc.setThreshold(info.threshold);

        layout.init(info, getActivity().getGlobalOptions());
        layoutInfo = new LayoutPosition();
        layout.reset(layoutInfo, info.pageNumber);
        layoutInfo.x.offset = info.newOffsetX;
        layoutInfo.y.offset = info.newOffsetY;

        lastScreenSize = new Point(info.screenWidth, info.screenHeight);
        screenOrientation = info.screenOrientation;
        changeOrinatation(screenOrientation);
        changeColorMode(info.colorMode, false);

        onDimensionChanged(width, height);

        Common.d("Controller inited");
    }

    public void serialize(LastPageInfo info) {
        layout.serialize(info);
        info.newOffsetX = layoutInfo.x.offset;
        info.newOffsetY = layoutInfo.y.offset;
        info.pageNumber = layoutInfo.pageNumber;
        info.screenOrientation = screenOrientation;
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

    public String getDirection() {
        return layout.getWalkOrder();
    }

    public int getLayout() {
        return layout.getLayout();
    }

    public void setDirectionAndLayout(String walkOrder, int pageLayout) {
        if (layout.changeNavigation(walkOrder) | layout.changePageLayout(pageLayout)) {
            sendViewChangeNotification();
        }
    }

    public void changetWalkOrder(String walkOrder) {
        if (layout.changeNavigation(walkOrder)) {
            sendViewChangeNotification();
        }
    }

    public void changetPageLayout(int pageLayout) {
        if (layout.changePageLayout(pageLayout)) {
            sendViewChangeNotification();
        }
    }

    public void changeContrast(int contrast) {
		if (this.contrast != contrast) {
			this.contrast = contrast;
			doc.setContrast(contrast);
			sendViewChangeNotification();
		}
    }

    public void changeThreshhold(int threshold) {
		if (this.threshold != threshold) {
			this.threshold = threshold;
			doc.setThreshold(threshold);
			sendViewChangeNotification();
		}
    }

    public OrionViewerActivity getActivity() {
        return activity;
    }

    public boolean isEvenPage() {
        //zero based
        return (getCurrentPage() + 1) % 2 == 0;
    }

    public void changeOrinatation(String orientationId) {
        screenOrientation = orientationId;
        System.out.println("New orientation " + screenOrientation);
        String realOrintationId = orientationId;
        if ("DEFAULT".equals(orientationId)) {
            realOrintationId = activity.getApplicationDefaultOrientation();
        }
        activity.changeOrientation(activity.getScreenOrientation(realOrintationId));
    }

    public void changeColorMode(String colorMode, boolean invalidate) {
        activity.getView().setColorMatrix(ColorUtil.getColorMode(colorMode));
        if (invalidate) {
            activity.getView().invalidate();
        }
    }

	public OutlineItem[] getOutline() {
		return doc.getOutline();
	}

    public String getScreenOrientation() {
        return screenOrientation;
    }

    public String selectText(int startX, int startY, int widht, int height) {
        Point leftTopCorner = layout.convertToPoint(layoutInfo);
        if (widht < 0) {
            startX += widht;
            widht = -widht;
        }
        if (height < 0) {
            startY += height;
            height = - height;
        }
        String text = doc.getText(layoutInfo.pageNumber, (int) ((leftTopCorner.x + startX) / layoutInfo.docZoom), (int) ((leftTopCorner.y +startY) / layoutInfo.docZoom), (int) (widht / layoutInfo.docZoom), (int) (height / layoutInfo.docZoom));
        if (text != null) {
            text = text.trim();
        }
        return text;
    }

    public boolean needPassword() {
        return doc.needPassword();
    }

    public boolean authentificate(String password) {
        boolean result = doc.authentificate(password);
        if (result) {
            sendViewChangeNotification();
        }
        return result;
    }

    public DocumentWrapper getDoc() {
        return doc;
    }

    public LayoutStrategy getLayoutStrategy() {
        return layout;
    }

}
