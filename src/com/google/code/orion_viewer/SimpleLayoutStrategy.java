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

import android.util.Log;

/**
 * User: mike
 * Date: 15.10.11
 * Time: 13:18
 */
public class SimpleLayoutStrategy implements LayoutStrategy {

    public static final int WIDTH = 600;

    public static final int HEIGHT = 760;

    public static final int OVERLAP = 15;

    private DocumentWrapper doc;

    private int leftMargin, topMargin, rightMargin, bottomMargin;

    private int zoom;

    private int rotation;

    public SimpleLayoutStrategy(DocumentWrapper doc) {
        this.doc = doc;
    }

    public void nextPage(LayoutPosition info) {
        int newX = info.offsetX + info.pieceWidth;
        int newY = info.offsetY;
        if (newX >= info.pageWidth) {
            newX = (int) (info.docZoom * leftMargin);
            newY = info.offsetY + info.pieceHeight;
            if (newY >= info.pageHeight) {
                if (info.pageNumber < doc.getPageCount() - 1) {
                    //go to next page with default position
                    reset(info, info.pageNumber + 1);
                    return;
                } else {
                    //last page && last view - do nothing
                    return;
                }
            } else {
                newY -= OVERLAP;
            }
        } else {
            newX -= OVERLAP;
        }

        info.offsetX = newX;
        info.offsetY = newY;
    }

    public void prevPage(LayoutPosition info) {
        int offsetX = info.offsetX - info.pieceWidth + OVERLAP;
        int offsetY = info.offsetY;
        Log.d(Common.LOGTAG, "new offsetX = " + offsetX + " maxX =" + info.pageWidth);

        if (offsetX < (int)(leftMargin * info.docZoom)) {
            int t = (info.pageWidth + info.pieceWidth - OVERLAP - OVERLAP) % (info.pieceWidth - OVERLAP);
            if (t == 0) {
                offsetX = (int) (info.pageWidth - info.pieceWidth + leftMargin * info.docZoom);
            } else {
                offsetX = (int) (info.pageWidth - t - OVERLAP + + leftMargin * info.docZoom);
            }
            Log.d(Common.LOGTAG, "new StartX = " + offsetX + " maxX =" + info.pageWidth);

            offsetY = info.offsetY - (rotation == 0 ? HEIGHT : WIDTH) + OVERLAP;

            Log.d(Common.LOGTAG, "new StartY = " + offsetY + " maxY =" + info.pageHeight);
            if (offsetY < (int) (topMargin * info.docZoom)) {
                if (info.pageNumber != 0) {
                    //recalculate with and height
                    reset(info, info.pageNumber - 1);
                    info.offsetX = -1;
                    info.offsetY = 3 * HEIGHT;
                    prevPage(info); //shift X

                    //offsetY = rotation == 0 ? HEIGHT : WIDTH;
                    t = (info.pageHeight + info.pieceHeight - OVERLAP - OVERLAP) % (info.pieceHeight - OVERLAP);
                    if (t == 0) {
                        offsetY = (int) (info.pageHeight - info.pieceHeight + topMargin * info.docZoom);
                    } else {
                        offsetY = (int) (info.pageHeight - t - OVERLAP + + topMargin * info.docZoom);
                    }
                    Log.d(Common.LOGTAG, "new StartY = " + offsetY + " maxX =" + info.pageHeight);
                } else {
                     reset(info, info.pageNumber);
                    return;
                }
            } else {

            }
        }

        info.offsetX = offsetX;
        info.offsetY = offsetY;

    }
    public boolean changeRotation(int rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
            return true;
        }
        return false;
    }

    private void resetNextPage(LayoutPosition info) {
        reset(info, info.pageNumber + 1);
    }

    private void resetPrevPage(LayoutPosition info) {
        reset(info, info.pageNumber - 1);
    }

    public void reset(LayoutPosition info, int pageNum) {
        info.pageNumber = pageNum;
        info.rotation = rotation;
        //original width and height without cropped margins
        PageInfo pinfo = doc.getPageInfo(pageNum);
        info.pageWidth = pinfo.width - leftMargin - rightMargin;
        info.pageHeight = pinfo.height - topMargin - bottomMargin;

        info.pieceWidth = rotation == 0 ? WIDTH : HEIGHT;
        info.pieceHeight = rotation == 0 ? HEIGHT : WIDTH;

        //calc zoom
        if (zoom <= 0) {
            //zoom by width
            info.docZoom = 1.0f * info.pieceWidth / info.pageWidth;
        } else {
            info.docZoom = 0.01f * zoom;
        }

        //zoomed with and height
        info.pageWidth = (int)(info.docZoom * info.pageWidth);
        info.pageHeight = (int) (info.docZoom * info.pageHeight);

        info.offsetX = (int)  (leftMargin * info.docZoom);
        info.offsetY = (int) (topMargin * info.docZoom);
    }

    public boolean changeZoom(int zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
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

    public int getRotation() {
        return rotation;
    }


    public void init(LastPageInfo info) {
        changeMargins(info.leftMargin, info.topMargin, info.rightMargin, info.bottomMargin);
        changeRotation(info.rotation);
        changeZoom(zoom);
    }

    public void serialize(LastPageInfo info) {
        info.leftMargin = leftMargin;
        info.rightMargin = rightMargin;
        info.topMargin = topMargin;
        info.bottomMargin = bottomMargin;
        info.rotation = rotation;
        info.zoom = zoom;
    }
}
