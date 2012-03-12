package com.google.code.orion_viewer;

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

    private static final int OVERLAP = 15;

    public int VERT_OVERLAP = 3;
    public int HOR_OVERLAP = 3;

    private DocumentWrapper doc;

    private int leftMargin, topMargin, rightMargin, bottomMargin;

    private int zoom;

    private int rotation;

    private int direction;

    private int layout;

    public SimpleLayoutStrategy(DocumentWrapper doc) {
        this.doc = doc;
    }

    public void nextPage(LayoutPosition info) {
        if (getDirection() == 0) {
            if (info.cellX < info.maxX) {
                info.cellX += 1;
            } else if (info.cellY < info.maxY) {
                info.cellX = 0;
                info.cellY += 1;
            } else {
                if (info.pageNumber < doc.getPageCount() - 1) {
                    reset(info, info.pageNumber + 1);
                }
            }
        } else {
            if (info.cellY < info.maxY) {
                info.cellY += 1;
            } else if (info.cellX < info.maxX) {
                info.cellY = 0;
                info.cellX += 1;
            } else {
                if (info.pageNumber < doc.getPageCount() - 1) {
                    reset(info, info.pageNumber + 1);
                }
            }
        }
        Common.d("new cellX = " + info.cellX + " cellY = " + info.cellY);
    }

    public void prevPage(LayoutPosition info) {
        if (getDirection() == 0) {
            if (info.cellX > 0) {
                info.cellX -= 1;
            } else if (info.cellY > 0) {
                info.cellX = info.maxX;
                info.cellY -= 1;
            } else {
                if (info.pageNumber > 0) {
                    reset(info, info.pageNumber - 1);
                    info.cellX = info.maxX;
                    info.cellY = info.maxY;
                }
            }
        } else {
            if (info.cellY > 0) {
                info.cellY -= 1;
            } else if (info.cellX > 0) {
                info.cellY = info.maxY;
                info.cellX -= 1;
            } else {
                if (info.pageNumber > 0) {
                    reset(info, info.pageNumber - 1);
                    info.cellX = info.maxX;
                    info.cellY = info.maxY;
                }
            }
        }

        Common.d("new cellX = " + info.cellX + " maxX = " + info.cellY);
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
        if (doc.getPageCount() - 1 < pageNum) {
            pageNum = doc.getPageCount() - 1;
        }
        if (pageNum < 0) {
            pageNum = 0;
        }

        info.pageNumber = pageNum;
        //original width and height without cropped margins
        PageInfo pinfo = doc.getPageInfo(pageNum);

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
            info.docZoom = 1.0f * info.pieceWidth / info.pageWidth;
        } else {
            info.docZoom = 0.01f * zoom;
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

        info.cellX = 0;
        info.cellY = 0;
    }

    public boolean changeZoom(int zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
            return true;
        }
        return false;
    }

    public boolean changeNavigation(int navigation) {
        if (this.direction != navigation) {
            this.direction = navigation;
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

    public boolean changeMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        if (leftMargin != this.leftMargin || rightMargin != this.rightMargin || topMargin != this.topMargin || bottomMargin != this.bottomMargin) {
            this.leftMargin = leftMargin;
            this.rightMargin = rightMargin;
            this.topMargin = topMargin;
            this.bottomMargin = bottomMargin;
            return true;
        }
        return false;
    }

    public void getMargins(int [] margins) {
        margins[0] = leftMargin;
        margins[1] = rightMargin;
        margins[2] = topMargin;
        margins[3] = bottomMargin;
    }

    public int getRotation() {
        return rotation;
    }


    public void init(LastPageInfo info, GlobalOptions options) {
        changeMargins(info.leftMargin, info.topMargin, info.rightMargin, info.bottomMargin);
        changeRotation(info.rotation);
        changeZoom(info.zoom);
        changeNavigation(info.navigation);
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
        info.rotation = rotation;
        info.zoom = zoom;
        info.navigation = direction;
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

    public int getDirection() {
        return direction;
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
