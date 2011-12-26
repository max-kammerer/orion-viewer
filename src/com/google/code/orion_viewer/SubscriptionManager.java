package com.google.code.orion_viewer;

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
}
