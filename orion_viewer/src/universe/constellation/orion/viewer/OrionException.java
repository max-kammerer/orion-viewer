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

import android.app.Activity;
import android.os.Message;

/**
 * User: mike
 * Date: 07.10.12
 * Time: 19:55
 */
public class OrionException extends Exception {

    public OrionException(Activity activity,int messageId, String subMessage) {
        this(activity.getResources().getString(messageId), subMessage);
    }

    public OrionException(String message, String subMessage) {
        super(message + ": " + subMessage);
    }

    public OrionException(String message, Exception e) {
        super(message + ": " + e.getMessage(), e);
    }

}
