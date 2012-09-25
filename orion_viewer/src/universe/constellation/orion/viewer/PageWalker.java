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

        public boolean isMinus() {
            return this == X_M || this == Y_M;
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

    public PageWalker(String direction) {
        WALK_ORDER [] values = WALK_ORDER.values();
        this.direction = WALK_ORDER.ABCD;
        for (int i = 0; i < values.length; i++) {
            WALK_ORDER value = values[i];
            if (value.code().equals(direction)) {
                this.direction = value;
                break;
            }
        }
    }

    public boolean next(LayoutPosition info) {
        return next(info, direction.first, direction.second);
    }

    public boolean prev(LayoutPosition info) {
        return next(info, direction.first.inverse(), direction.second.inverse());
    }

    //true if should increase page
    public boolean next(LayoutPosition info, DIR firstDir, DIR secondDir) {
        int first = info.cellX;
        int firstMax = info.maxX;

        int second = info.cellY;
        int secondMax = info.maxY;

        if  (!firstDir.isX()) {
            first = info.cellY;
            firstMax = info.maxY;
            second = info.cellX;
            secondMax = info.maxX;
        }

        firstMax++;
        secondMax++;

        first = first + firstDir.delta + firstMax;
        int changeF = first / firstMax;
        first %= firstMax;

        if (changeF != 1) {
            second = second + secondDir.delta + secondMax;
            int changeS = second / secondMax;
            second %= secondMax;
            if (changeS != 1) {
                return true;
            }
        }

        info.cellX = firstDir.isX() ? first : second;
        info.cellY = secondDir.isX() ? first : second;

        return false;
    }

    public void reset(LayoutPosition info, boolean isNext) {
        DIR first = isNext ? direction.first : direction.first.inverse();
        DIR second = isNext ? direction.second : direction.second.inverse();

        DIR xDir = first.isX() ? first : second;
        DIR yDir = second.isX() ? first : second;
        info.cellX = xDir.isMinus() ? info.maxX : 0;
        info.cellY = yDir.isMinus() ? info.maxY : 0;
    }

    public String getDirection() {
        return direction.name();
    }

    public boolean isLeft2Right() {
        return direction == WALK_ORDER.ABCD || direction == WALK_ORDER.ACBD;
    }
}
