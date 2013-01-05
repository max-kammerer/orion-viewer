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

/**
 * User: mike
 * Date: 29.08.12
 * Time: 12:58
 */
public class PageWalker {

    public static enum DIR {
        //X, Y, -X, -Y
        X (1), Y(1), X_M(-1), Y_M(-1);

        private final int delta;

        DIR(int delta) {
            this.delta = delta;
        }

        public DIR inverse() {
            switch (this) {
                case X : return X_M;
                case X_M: return X;
                case Y: return Y_M;
                case Y_M: return Y;
            }
            return X;
        }

        public boolean isX() {
            return this == X_M || this == X;
        }

        public boolean toLeftOrUp() {
            return this == X_M || this == Y_M;
        }

        public boolean toRightOrDown() {
            return this == X || this == Y;
        }
    }

    public static enum WALK_ORDER {

        ABCD (0, DIR.X, DIR.Y), ACBD (1, DIR.Y, DIR.X), BADC(2, DIR.X_M, DIR.Y), BDAC (3, DIR.Y, DIR.X_M);

        private final DIR first;

        private final DIR second;

        WALK_ORDER(int code, DIR first, DIR second) {
            this.first = first;
            this.second = second;
        }

        public String code() {
            return this.name();
        }
    };

    private WALK_ORDER direction;

    private SimpleLayoutStrategy layout;

    public PageWalker(String direction, SimpleLayoutStrategy layout) {
        WALK_ORDER [] values = WALK_ORDER.values();
        this.direction = WALK_ORDER.ABCD;
        for (int i = 0; i < values.length; i++) {
            WALK_ORDER value = values[i];
            if (value.code().equals(direction)) {
                this.direction = value;
                break;
            }
        }
        this.layout = layout;
    }

    public boolean next(LayoutPosition info) {
        return next(info, direction.first, direction.second);
    }

    public boolean prev(LayoutPosition info) {
        return next(info, direction.first.inverse(), direction.second.inverse());
    }

    //true if should show prev/next page
    public boolean next(LayoutPosition info, DIR firstDir, DIR secondDir) {
        boolean isX = firstDir.isX();
        OneDimension first = isX ? info.x : info.y;
        OneDimension second = isX ? info.y : info.x;

        int firstOffset = first.offset;
        int secondOffset = second.offset;

        boolean changeSecond = false;

        int newOffsetStart = firstOffset + first.screenDimension * firstDir.delta;
        int newOffsetEnd = newOffsetStart + first.screenDimension - 1;

        if (!inInterval(newOffsetStart, first) && !inInterval(newOffsetEnd, first)) {
            changeSecond = true;
            firstOffset = reset(firstDir, first, 0);
        } else {
            if (needAlign(firstDir) && (!inInterval(newOffsetStart, first) || !inInterval(newOffsetEnd, first))) {
                firstOffset = align(firstDir, first);
            } else {
                firstOffset = newOffsetStart - first.overlap * firstDir.delta;
            }
        }

        int newValueFirst = firstOffset;

        int newValueSecond = secondOffset;
        if (changeSecond) {
            changeSecond = false;
            firstOffset = secondOffset;
            first = second;
            firstDir = secondDir;

            newOffsetStart = firstOffset + first.screenDimension * firstDir.delta;
            newOffsetEnd = newOffsetStart + first.screenDimension - 1;

            if (!inInterval(newOffsetStart, first) && !inInterval(newOffsetEnd, first)) {
                changeSecond = true;
                firstOffset = reset(firstDir, first, 0);
            } else {
                if (needAlign(firstDir) && (!inInterval(newOffsetStart, first) || !inInterval(newOffsetEnd, first))) {
                    firstOffset = align(firstDir, first);
                } else {
                    firstOffset = newOffsetStart - first.overlap * firstDir.delta;
                }
            }
            newValueSecond = firstOffset;
        }

        if (!changeSecond) {
            (isX ? info.x : info.y).offset = newValueFirst;
            second.offset = newValueSecond;
        }

        return changeSecond;
    }

    private int reset(DIR dir, OneDimension dim, int possibleOffset) {
        if (dir.toRightOrDown() || dim.pageDimension <= dim.screenDimension) {
            return 0;
        } else {
            if (!needAlign(dir)) {
                return  ((dim.pageDimension - dim.overlap) / (dim.screenDimension - dim.overlap)) * (dim.screenDimension - dim.overlap);
            } else {
                return dim.pageDimension - dim.screenDimension;
            }
        }
    }


    private int align(DIR dir, OneDimension dim) {
        if (dir.toRightOrDown()) {
            return dim.pageDimension - dim.screenDimension;
        } else {
            return 0;
        }
    }

    private boolean needAlign(DIR dir) {
        return dir.isX() && layout.getLayout() != 0 || (!dir.isX() && layout.getLayout() == 1);
    }

    private boolean inInterval(int value, OneDimension dim) {
        return value >= 0 && value < dim.pageDimension;
    }


    public void reset(LayoutPosition info, boolean isNext) {
//        if (info.screenHeight != 0 && info.screenWidth != 0) {
//            info.maxX = (info.pageWidth - hOverlap) / (info.getRenderWidth() - hOverlap) + ((info.pageWidth - hOverlap) % (info.getRenderWidth() - hOverlap) == 0 ? 0 : 1)  - 1;
//            info.maxY = (info.pageHeight - vOverlap) / (info.getRenderHeight() - vOverlap) + ((info.pageHeight - vOverlap) % (info.getRenderHeight() - vOverlap) == 0 ?  0: 1) -1;
//
//            //in this case page size is smaller than overlap and it smaller than screen size - so there is only one possibility
//            if (info.maxX < 0)  {
//                info.maxX = 0;
//            }
//
//            if (info.maxY < 0)  {
//                info.maxY = 0;
//            }
//        }
//
        DIR first = isNext ? direction.first : direction.first.inverse();
        DIR second = isNext ? direction.second : direction.second.inverse();
//
//        DIR xDir = first.isX() ? first : second;
//        DIR yDir = second.isX() ? first : second;

        OneDimension hor =  first.isX() ? info.getHor() : info.getVert();
        OneDimension vert =  first.isX() ? info.getVert() : info.getHor();

        reset(first, hor, 0);
        reset(second, vert, 0);
//        hor.offset = first.toLeftOrUp() ? hor.pageDimension - hor.screenDimension : 0;
//        vert.offset = second.toLeftOrUp() ? vert.pageDimension - vert.screenDimension : 0;
        hor.offset = reset(first, hor, 0);
        vert.offset = reset(second, vert, 0);

//        info.cellX = xDir.isMinus() ? info.maxX : 0;
//        info.cellY = yDir.isMinus() ? info.maxY : 0;


    }

    public String getDirection() {
        return direction.name();
    }

}
