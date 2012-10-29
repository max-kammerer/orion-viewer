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

import android.graphics.Point;
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

    private int leftMargin, topMargin, rightMargin, bottomMargin, leftEvenMargin, rightEvenMargin;

    private boolean enableEvenCrop;

    private int zoom;

    private int rotation;

    private PageWalker walker = new PageWalker("default");

    private int layout;

    public SimpleLayoutStrategy(DocumentWrapper doc, Point deviceSize) {
        this.doc = doc;
    }

    public void nextPage(LayoutPosition info) {
        if (walker.next(info)) {
            if (info.pageNumber < doc.getPageCount() - 1) {
                reset(info, info.pageNumber + 1);
            }
        }
        Common.d("new cellX = " + info.cellX + " cellY = " + info.cellY);
    }

    public void prevPage(LayoutPosition info) {
        if (walker.prev(info)) {
            if (info.pageNumber > 0) {
                reset(info, info.pageNumber - 1, false);
            }
        }

        Common.d("new cellX = " + info.cellX + " cellY = " + info.cellY);
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

        info.pageNumber = pageNum;
        //original width and height without cropped margins
        PageInfo pinfo = doc.getPageInfo(pageNum);

        boolean isEvenPage = (pageNum + 1) % 2 == 0;
        int leftMargin = enableEvenCrop && isEvenPage ? leftEvenMargin : this.leftMargin;
        int rightMargin = enableEvenCrop && isEvenPage ? rightEvenMargin : this.rightMargin;

        info.marginLeft = (int) (leftMargin * pinfo.width * 0.01);
        info.marginTop = (int) (topMargin * pinfo.height * 0.01);

        info.pageWidth = Math.round(pinfo.width * (1 - 0.01f *(leftMargin + rightMargin)));
        info.pageHeight = Math.round(pinfo.height * (1- 0.01f *(topMargin + bottomMargin)));

        info.pieceWidth = rotation == 0 ? viewWidth : viewHeight;
        info.pieceHeight = rotation == 0 ? viewHeight : viewWidth;

        info.screenWidth = viewWidth;
        info.screenHeight = viewHeight;

        //calc zoom
        if (zoom <= 0) {
            //zoom by width
            switch (zoom) {
                case 0: info.docZoom = ((double) info.pieceWidth) / info.pageWidth; break;
                case -1: info.docZoom = ((double)info.pieceHeight) / info.pageHeight; break;
                case -2: info.docZoom = Math.min(((double ) info.pieceWidth) / info.pageWidth, ((double)info.pieceHeight) / info.pageHeight); break;
            }
        } else {
            info.docZoom = 0.0001f * zoom;
        }
        info.marginLeft = (int) (info.docZoom * info.marginLeft);
        info.marginTop = (int) (info.docZoom * info.marginTop);

        //zoomed with and height
        info.pageWidth = (int)(info.docZoom * info.pageWidth);
        info.pageHeight = (int) (info.docZoom * info.pageHeight);

        int hOverlap = info.pieceWidth * HOR_OVERLAP / 100;
        int vOverlap = info.pieceHeight * VERT_OVERLAP / 100;
        //System.out.println("overlap " + hOverlap + " " + vOverlap);
        if (info.pieceHeight != 0 && info.pieceWidth != 0) {
            info.maxX = (info.pageWidth - hOverlap) / (info.pieceWidth - hOverlap) + ((info.pageWidth - hOverlap) % (info.pieceWidth - hOverlap) == 0 ? 0 : 1)  - 1;
            info.maxY = (info.pageHeight - vOverlap) / (info.pieceHeight - vOverlap) + ((info.pageHeight - vOverlap) % (info.pieceHeight - vOverlap) == 0 ?  0: 1) -1;
        }

        walker.reset(info, forward);
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
            walker = new PageWalker(walkOrder);
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

    public boolean changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin, boolean enableEven, int leftEvenMargin, int rightEvenMargin) {
        if (leftMargin != this.leftMargin || rightMargin != this.rightMargin || topMargin != this.topMargin || bottomMargin != this.bottomMargin
                || leftEvenMargin != this.leftEvenMargin || rightEvenMargin != this.rightEvenMargin || this.enableEvenCrop != enableEven) {
            this.leftMargin = leftMargin;
            this.rightMargin = rightMargin;
            this.topMargin = topMargin;
            this.bottomMargin = bottomMargin;
            this.leftEvenMargin = leftEvenMargin;
            this.rightEvenMargin = rightEvenMargin;
            this.enableEvenCrop = enableEven;
            return true;
        }
        return false;
    }


    public boolean isEnableEvenCrop() {
        return enableEvenCrop;
    }

    public void getMargins(int [] margins) {
        margins[0] = leftMargin;
        margins[1] = rightMargin;
        margins[2] = topMargin;
        margins[3] = bottomMargin;

        margins[4] = leftEvenMargin;
        margins[5] = rightEvenMargin;
    }

    public int getRotation() {
        return rotation;
    }


    public void init(LastPageInfo info, GlobalOptions options) {
        changeMargins(info.leftMargin, info.topMargin, info.rightMargin, info.bottomMargin, info.enableEvenCropping, info.leftEvenMargin, info.rightEventMargin);
        changeRotation(info.rotation);
        changeZoom(info.zoom);
        changeNavigation(info.walkOrder);
        changePageLayout(info.pageLayout);
        changeOverlapping(options.getHorizontalOverlapping(), options.getVerticalOverlapping());
    }

    public void serialize(LastPageInfo info) {
        info.screenHeight = viewHeight;
        info.screenWidth = viewWidth;

        info.leftMargin = leftMargin;
        info.rightMargin = rightMargin;
        info.topMargin = topMargin;
        info.bottomMargin = bottomMargin;
        info.leftEvenMargin = leftEvenMargin;
        info.rightEventMargin = rightEvenMargin;
        info.enableEvenCropping = enableEvenCrop;

        info.rotation = rotation;
        info.zoom = zoom;
        info.walkOrder = walker.getDirection();
        info.pageLayout = layout;
    }

    public Point convertToPoint(LayoutPosition pos) {
        int layout  = getLayout();
//        int absLeftMargin = (int) (leftMargin * pos.pageWidth * 0.01);
//        int absTopMargin = (int) (topMargin * pos.pageHeight * 0.01);
        int absLeftMargin = (int) pos.marginLeft;
        int absTopMargin = (int) pos.marginTop;
        int hOverlap = pos.pieceWidth * HOR_OVERLAP / 100;
        int vOverlap = pos.pieceHeight * VERT_OVERLAP / 100;

        int x = pos.cellX == 0 || pos.cellX != pos.maxX || layout == 0 ?  (int)(absLeftMargin + pos.cellX * (pos.pieceWidth - hOverlap)) : (int) (absLeftMargin + pos.pageWidth - pos.pieceWidth);
        int y = pos.cellY == 0 || pos.cellY != pos.maxY || layout != 1 ? (int) (absTopMargin + pos.cellY * (pos.pieceHeight - vOverlap)) : (int) (absTopMargin + pos.pageHeight - pos.pieceHeight);

        return new Point(x, y);
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

    public int getLeftMargin() {
        return leftMargin;
    }

    public int getTopMargin() {
        return topMargin;
    }

    public int getRightMargin() {
        return rightMargin;
    }

    public int getBottomMargin() {
        return bottomMargin;
    }
}
