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

        public boolean isLeftToRight() {
            return this == ABCD || this == ACBD;
        }
    };

    private boolean doCentering = true;

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
    private boolean next(LayoutPosition info, DIR firstDir, DIR secondDir) {
        boolean isX = firstDir.isX();
        OneDimension first = isX ? info.x : info.y;
        OneDimension second = isX ? info.y : info.x;

        boolean changeSecond = true;
        int newFirstOffset = -1;
        int newSecondOffset = -1;

        for (int i = 1; i <= 2; i ++) {
            OneDimension dimension = i == 1 ? first : second;
            DIR dir = i == 1 ? firstDir : secondDir;

            boolean inverseX = dir.isX() && !direction.isLeftToRight();
            int offset = inverseX ? dimension.pageDimension - dimension.offset - dimension.screenDimension: dimension.offset;
            dir = inverseX ? dir.inverse() : dir;

            if (changeSecond) {
                changeSecond = false;

                int newOffsetStart = offset + dimension.screenDimension * dir.delta;
                int newOffsetEnd = newOffsetStart + dimension.screenDimension - 1;

                if (!inInterval(newOffsetStart, dimension) && !inInterval(newOffsetEnd, dimension)) {
                    changeSecond = true;
                    offset = reset(dir, dimension, true);
                } else {
                    if (needAlign(dir) && (!inInterval(newOffsetStart, dimension) || !inInterval(newOffsetEnd, dimension))) {
                        offset = align(dir, dimension);
                    } else {
                        offset = newOffsetStart - dimension.overlap * dir.delta;
                    }
                }
            }

            offset = inverseX ? dimension.pageDimension - offset - dimension.screenDimension: offset;

            if (i == 1) {
                newFirstOffset = offset;
            } else {
                newSecondOffset = offset;
            }
        }

        if (!changeSecond) {
            (isX ? info.x : info.y).offset = newFirstOffset;
            second.offset = newSecondOffset;
        }

        return changeSecond;
    }

    private int reset(DIR dir, OneDimension dim, boolean doCentering) {
        if (this.doCentering && doCentering) {
            if (dim.pageDimension < dim.screenDimension) {
                return (dim.pageDimension - dim.screenDimension) / 2;
            }
        }

        if (dir.toRightOrDown() || dim.pageDimension <= dim.screenDimension || dim.screenDimension == 0) {
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


    public void reset(LayoutPosition info, boolean isNext, boolean doCentering) {
        DIR first = isNext ? direction.first : direction.first.inverse();
        DIR second = isNext ? direction.second : direction.second.inverse();

        DIR horDir = first.isX() ? first : second;
        DIR vertDir = first.isX() ? second : first;

        boolean inverse = !direction.isLeftToRight();
        horDir = inverse ? horDir.inverse() : horDir;
        info.x.offset = reset(horDir, info.x, doCentering);
        info.x.offset = inverse ? info.x.pageDimension - info.x.screenDimension - info.x.offset : info.x.offset;

        info.y.offset = reset(vertDir, info.y, doCentering);
    }

    public String getDirection() {
        return direction.name();
    }

}
