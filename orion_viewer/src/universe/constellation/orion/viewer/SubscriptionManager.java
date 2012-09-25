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

import java.util.ArrayList;
import java.util.List;

/**
 * User: mike
 * Date: 26.12.11
 * Time: 13:35
 */
public class SubscriptionManager {

    private List<DocumentViewListener> listeners = new ArrayList<DocumentViewListener>();


     public void addDocListeners(DocumentViewListener listeners) {
        this.listeners.add(listeners);
    }

    public void sendViewChangeNotification() {
        for (int i = 0; i < listeners.size(); i++) {
            DocumentViewListener documentListener =  listeners.get(i);
            documentListener.viewParametersChanged();
        }
    }

    public void sendPageChangedNotification(int newPage, int pageCount) {
        for (int i = 0; i < listeners.size(); i++) {
            DocumentViewListener documentListener =  listeners.get(i);
            documentListener.pageChanged(newPage, pageCount);
        }
    }

    public void sendDocOpenedNotification(Controller controller) {
        for (int i = 0; i < listeners.size(); i++) {
            DocumentViewListener documentListener =  listeners.get(i);
            documentListener.documentOpened(controller);
        }
    }

    public void unSubscribe(DocumentViewListener listener) {
        listeners.remove(listener);
    }
}
