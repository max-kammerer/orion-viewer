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

import universe.constellation.orion.viewer.document.DocumentWithCaching;
import universe.constellation.orion.viewer.prefs.GlobalOptions;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 13:18
 */
public class SimpleLayoutStrategy implements LayoutStrategy {

    public int viewWidth;

    public int viewHeight;

    public int VERT_OVERLAP = 3;

    public int HOR_OVERLAP = 3;

    private DocumentWrapper doc;

    private CropMargins cropMargins = new CropMargins(0, 0, 0, 0, 0, 0, false, 0);

    private int zoom;

    private int rotation;

    private PageWalker walker = new PageWalker("default", this);

    private int layout;

    public SimpleLayoutStrategy(DocumentWrapper doc) {
        this.doc = doc;
        //TODO: ugly hack
        if (doc instanceof DocumentWithCaching) {
            ((DocumentWithCaching) doc).strategy = this;
        }
    }

    public void nextPage(LayoutPosition info) {
        if (walker.next(info)) {
            if (info.pageNumber < doc.getPageCount() - 1) {
                reset(info, info.pageNumber + 1);
            }
        }
        Common.d("new cellX = " + info.x.offset + " cellY = " + info.y.offset);
    }

    public void prevPage(LayoutPosition info) {
        if (walker.prev(info)) {
            if (info.pageNumber > 0) {
                reset(info, info.pageNumber - 1, false);
            }
        }

        Common.d("new cellX = " + info.x.offset + " cellY = " + info.y.offset);
    }

    public boolean changeRotation(int rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
            return true;
        }
        return false;
    }

    public boolean changeOverlapping(int horizontal, int vertical) {
        if (HOR_OVERLAP != horizontal || VERT_OVERLAP != vertical) {
            HOR_OVERLAP = horizontal;
            VERT_OVERLAP = vertical;
            return true;
        }
        return false;
    }

    public void reset(LayoutPosition info, int pageNum) {
        reset(info, pageNum, true);
    }

    public void reset(LayoutPosition info, int pageNum, boolean forward) {
        if (doc.getPageCount() - 1 < pageNum) {
            pageNum = doc.getPageCount() - 1;
        }
        if (pageNum < 0) {
            pageNum = 0;
        }

        //original width and height without cropped margins
        reset(info, forward, doc.getPageInfo(pageNum, cropMargins.cropMode), cropMargins.cropMode, zoom);
    }

    @Override
    public void reset(LayoutPosition info, boolean forward, PageInfo pageInfo, int cropMode, int zoom) {
        info.rotation = rotation;
        info.pageNumber = pageInfo.pageNum0;

        int pageWidth = pageInfo.width;
        int pageHeight = pageInfo.height;
        resetMargins(info, pageWidth, pageHeight);

        boolean isEvenPage = (pageInfo.pageNum0 + 1) % 2 == 0;
        CropMode mode = CropMarginsKt.getToMode(cropMode);

        AutoCropMargins autoCrop = pageInfo.autoCrop;
        if (autoCrop != null && CropMode.AUTO_MANUAL == mode) {
            appendAutoCropMargins(info, autoCrop);
        }

        if (CropMarginsKt.hasManual(mode)) {
            int leftMargin = cropMargins.evenCrop && isEvenPage ? cropMargins.evenLeft : cropMargins.left;
            int rightMargin = cropMargins.evenCrop && isEvenPage ? cropMargins.evenRight : cropMargins.right;
            appendManualMargins(info, leftMargin, rightMargin);
        }

        if (autoCrop != null && CropMarginsKt.hasAuto(mode) && CropMode.AUTO_MANUAL != mode) {
            appendAutoCropMargins(info, autoCrop);
        }

        info.x.screenDimension = rotation == 0 ? viewWidth : viewHeight;
        info.y.screenDimension = rotation == 0 ? viewHeight : viewWidth;

        info.screenWidth = viewWidth;
        info.screenHeight = viewHeight;

        //set zoom and zoom margins and dimensions
        info.setDocZoom(zoom);

        info.x.marginLess = (int) (info.docZoom * info.x.marginLess);
        info.y.marginLess = (int) (info.docZoom * info.y.marginLess);

        //zoomed with and height
        info.x.pageDimension = (int) (info.docZoom * info.x.pageDimension);
        info.y.pageDimension = (int) (info.docZoom * info.y.pageDimension);

        info.x.overlap = info.x.screenDimension * HOR_OVERLAP / 100;
        info.y.overlap = info.y.screenDimension * VERT_OVERLAP / 100;
        //System.out.println("overlap " + hOverlap + " " + vOverlap);

        walker.reset(info, forward);
    }

    public void appendManualMargins(LayoutPosition info, int leftMargin, int rightMargin) {
        int pageWidth = info.x.pageDimension;
        int pageHeight = info.y.pageDimension;

        int xLess = (int) (leftMargin * pageWidth * 0.01);
        int xMore = (int) (pageWidth * rightMargin * 0.01);
        int yLess = (int) (cropMargins.top * pageHeight * 0.01);
        int yMore = (int) (pageHeight * cropMargins.bottom * 0.01);

        info.x.marginLess += xLess;
        info.x.marginMore += xMore;
        info.y.marginLess += yLess;
        info.y.marginMore += yMore;

        info.x.pageDimension -= xLess + xMore;
        info.y.pageDimension -= yLess + yMore;
    }

    public void appendAutoCropMargins(LayoutPosition info, AutoCropMargins autoCrop) {
        info.x.marginLess += autoCrop.left;
        info.x.marginMore += autoCrop.right;
        info.y.marginLess += autoCrop.top;
        info.y.marginMore += autoCrop.bottom;

        info.x.pageDimension -= autoCrop.left + autoCrop.right;
        info.y.pageDimension -= autoCrop.top + autoCrop.bottom;
    }

    public void resetMargins(LayoutPosition info, int pageWidth, int pageHeight) {
        info.x.marginLess = 0;
        info.y.marginLess = 0;
        info.x.marginMore = 0;
        info.y.marginMore = 0;
        info.x.pageDimension = pageWidth;
        info.y.pageDimension = pageHeight;
    }

    public boolean changeZoom(int zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
            return true;
        }
        return false;
    }

    public boolean changeNavigation(String walkOrder) {
        if (walkOrder != null && !walkOrder.equals(walker.getDirection())) {
            walker = new PageWalker(walkOrder, this);
            return true;
        }
        return false;
    }

    public boolean changePageLayout(int navigation) {
        if (this.layout != navigation) {
            this.layout = navigation;
            return true;
        }
        return false;
    }

    public int getZoom() {
        return zoom;
    }

    public boolean changeCropMargins(CropMargins margins) {
        if (!margins.equals(cropMargins)) {
            cropMargins = margins;
            return true;
        }

        return false;
    }


    public CropMargins getMargins() {
        return cropMargins;
    }

    public int getRotation() {
        return rotation;
    }


    public void init(LastPageInfo info, GlobalOptions options) {
        changeCropMargins(new CropMargins(info.leftMargin, info.rightMargin, info.topMargin, info.bottomMargin, info.leftEvenMargin, info.rightEventMargin, info.enableEvenCropping, info.cropMode));
        changeRotation(info.rotation);
        changeZoom(info.zoom);
        changeNavigation(info.walkOrder);
        changePageLayout(info.pageLayout);
        changeOverlapping(options.getHorizontalOverlapping(), options.getVerticalOverlapping());
    }

    public void serialize(LastPageInfo info) {
        info.screenHeight = viewHeight;
        info.screenWidth = viewWidth;

        info.leftMargin = cropMargins.left;
        info.rightMargin = cropMargins.right;
        info.topMargin = cropMargins.top;
        info.bottomMargin = cropMargins.bottom;
        info.leftEvenMargin = cropMargins.evenLeft;
        info.rightEventMargin = cropMargins.evenRight;
        info.enableEvenCropping = cropMargins.evenCrop;
        info.cropMode = cropMargins.cropMode;

        info.rotation = rotation;
        info.zoom = zoom;
        info.walkOrder = walker.getDirection();
        info.pageLayout = layout;
    }

    public Point convertToPoint(LayoutPosition pos) {
        return new Point(pos.x.marginLess + pos.x.offset, pos.y.marginLess + pos.y.offset);
    }

    public int getLayout() {
        return layout;
    }

    public String getWalkOrder() {
        return walker.getDirection();
    }

    public void setDimension(int width, int height) {
        viewWidth = width;
        viewHeight = height;
    }

    @Override
    public PageWalker getWalker() {
        return walker;
    }
}